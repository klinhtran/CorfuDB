/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.corfudb.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class StreamEntryImpl implements StreamEntry
{
    private ITimestamp logpos; //this doesn't have to be serialized, but leaving it in for debug purposes
    private Object payload;
    private Set<Long> streams;

    public ITimestamp getLogpos()
    {
        return logpos;
    }

    public Object getPayload()
    {
        return payload;
    }

    public Set<Long> getStreams()
    {
        return streams;
    }

    public StreamEntryImpl(Object tbs, Timestamp position, Set<Long> tstreams)
    {
        logpos = position;
        payload = tbs;
        streams = tstreams;
    }
}



class StreamFactoryImpl implements StreamFactory
{
    WriteOnceAddressSpace was;
    StreamingSequencer ss;
    public StreamFactoryImpl(WriteOnceAddressSpace twas, StreamingSequencer tss)
    {
        was = twas;
        ss = tss;
    }
    public Stream newStream(long streamid)
    {
        return new StreamImpl(streamid, ss, was);
    }

}

class StreamImpl implements Stream
{
    static Logger dbglog = LoggerFactory.getLogger(StreamImpl.class);

    long streamid;

    StreamingSequencer seq;
    WriteOnceAddressSpace addrspace;

    Lock biglock;
    long curpos;
    long curtail;


    public long getStreamID()
    {
        return streamid;
    }

    StreamImpl(long tstreamid, StreamingSequencer tss, WriteOnceAddressSpace tlas)
    {
        streamid = tstreamid;
        seq = tss;
        addrspace = tlas;
        biglock = new ReentrantLock();

    }

    @Override
    public ITimestamp append(Serializable payload, Set<Long> streams)
    {
        long ret = seq.get_slot(streams);
        Timestamp T = new Timestamp(addrspace.getID(), ret, 0, this.getStreamID()); //todo: fill in the right epoch
        dbglog.debug("reserved slot {}", ret);
        StreamEntry S = new StreamEntryImpl(payload, T, streams);
        addrspace.write(ret, BufferStack.serialize(S));
        dbglog.debug("wrote slot {}", ret);
        return T;
    }

    @Override
    public StreamEntry readNext()
    {
        return readNext(null);
    }

    @Override
    public StreamEntry readNext(ITimestamp istoppos)
    {
        //this is a hacky implementation that doesn't take multi-log hopping (epochs, logids) into account
        Timestamp stoppos = (Timestamp)istoppos;
        if(stoppos!=null && stoppos.logid!=addrspace.getID()) throw new RuntimeException("readnext using timestamp of different log!");
        StreamEntry ret = null;
        while(true)
        {
            biglock.lock();
            if (!(curpos < curtail && (stoppos == null || curpos < stoppos.pos)))
            {
                biglock.unlock();
                return null;
            }
            long readpos = curpos++;
            biglock.unlock();
            BufferStack bs = addrspace.read(readpos);
            ret = (StreamEntry) bs.deserialize();
            if(ret.getStreams().contains(this.getStreamID()))
                break;
            dbglog.debug("skipping...");
        }
        return ret;
    }

    @Override
    public ITimestamp checkTail()
    {
        long tcurtail = seq.check_tail();
        biglock.lock();
        if(tcurtail>curtail) curtail = tcurtail;
        biglock.unlock();
        return new Timestamp(addrspace.getID(), tcurtail, 0, this.getStreamID()); //todo: populate epoch
    }

    @Override
    public void prefixTrim(ITimestamp trimpos)
    {
        throw new RuntimeException("unimplemented");
    }
}