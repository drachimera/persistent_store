/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.charset.Charset;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

/**
 *
 * @author Daniel Quest
 * 
 * Implements a buffer that provides a slightly more efficient way of writing, and then reading a stream of bytes. To
 * use: PipeChannel pipe = new PipeChannel(); byte[] bytes = {'a','b','c','d'}; pipe.write(bytes); pipe.close();
 * InputStream in = pipe.getInputStream(); int i = -1; while ((i = in.read()) != -1) {...} By default, closing will
 * automatically cause it to flip over to Read mode, locking the buffer from further writes and setting the read
 * position to 0. Once the Buffer has been fully read, it must be reset, which sets it back into write mode
 * 
 * 
 * This class helps us convert one type of streaming data to another.
 * The basic model is as follows:
 * 
 *                      PIPE
 *              ______________________ 
 *  ______     |   _____      _____   |     ____
 *  |    |     |  |     |    |     |  |    |    |
 *  |    |-----|->|     |--->|     |--|--->|    |
 *  |____|     |  |_____|    |_____|  |    |____|
 *  ThreadA    |   Sink       Source  |    ThreadB
 *             |  Channel    Channel  |
 *             |______________________|
 * In this model, ThreadA is called the producer
 *                ThreadB is called the consumer
 *      
 * A Java NIO Pipe is a one-way data connection between two (perhaps non-blocking) threads. 
 * A Pipe has a source channel and a sink channel. 
 * You write data to the sink channel. 
 * This data can then be read from the source channel.
 */
public class StreamingDataPocketKnife implements ReadableByteChannel, WritableByteChannel, Appendable, Readable, Closeable {
    
    protected String charset = Charset.defaultCharset().name();
    protected Pipe pipe;
    protected boolean flipped = false;    
    public StreamingDataPocketKnife() {
        reset();
    }

    public StreamingDataPocketKnife(String charset) {
        this.charset = charset;
    }
    
    /**
     * producer related methods
     * @return 
     */
    /**
     * To write to the pocketknife (pipe) you need to access the sink channel. 
     * This method will give you that object directly
     */
    public Pipe.SinkChannel getSink(){ return pipe.sink(); }
    public OutputStream getProducerOutputStream(){
        return this.getOutputStream();
    }

    /**
     * consumer related methods
     * @return 
     */ 
    public InputStream getConsumerInputStream(){
        return this.getInputStream();
    }
    
    /**

     */
    private void checkFlipped() {
        if (flipped)
            throw new RuntimeException("PipeChannel is read only");
    }

    private void checkNotFlipped() {
        if (!flipped)
            throw new RuntimeException("PipeChannel is write only");
    }

    /**
     * Get an inputstream that can read from this pipe. The Pipe must be readable
     */
    public InputStream getInputStream() {
        checkNotFlipped();
        return new PipeChannelInputStream(pipe, Channels.newInputStream(pipe.source()));
    }

    /**
     * Get an outputstream that can write to this pipe. The Pipe must be writable
     */
    public OutputStream getOutputStream() {
        checkFlipped();
        return new PipeChannelOutputStream(pipe, Channels.newOutputStream(pipe.sink()));
    }

    /**
     * Get a writer that can write to this pipe. The pipe must be writable
     */
    public Writer getWriter() {
        checkFlipped();
        return new PipeChannelWriter(pipe, Channels.newWriter(pipe.sink(), charset));
    }

    /**
     * Get a writer that can write to this pipe. The pipe must be writable
     */
    public Writer getWriter(String charset) {
        checkFlipped();
        return new PipeChannelWriter(pipe, Channels.newWriter(pipe.sink(), charset));
    }

    /**
     * Get a reader that can reader from this pipe. The pipe must be readable
     */
    public Reader getReader(String charset) {
        checkNotFlipped();
        return new PipeChannelReader(pipe, Channels.newReader(pipe.source(), charset));
    }

    /**
     * Get a reader that can reader from this pipe. The pipe must be readable
     */
    public Reader getReader() {
        checkNotFlipped();
        return new PipeChannelReader(pipe, Channels.newReader(pipe.source(), charset));
    }

    /**
     * Read from the pipe.
     */
    public int read(ByteBuffer dst) throws IOException {
        checkNotFlipped();
        return pipe.source().read(dst);
    }

    /**
     * Read from the pipe.
     */
    public int read(byte[] dst) throws IOException {
        checkNotFlipped();
        return pipe.source().read(ByteBuffer.wrap(dst));
    }

    /**
     * Read from the pipe.
     */
    public int read(byte[] dst, int offset, int length) throws IOException {
        checkNotFlipped();
        return pipe.source().read(ByteBuffer.wrap(dst, offset, length));
    }

    /**
     * True if the pipe is open
     */
    public boolean isOpen() {
        return pipe.sink().isOpen() || pipe.source().isOpen();
    }

    
    public void write(String s) throws IOException{
        ByteBuffer buf = ByteBuffer.allocate(s.length()+1);
        buf.clear();
        buf.put(s.getBytes());
        buf.flip();

        while(buf.hasRemaining()) {
            write(buf);
        }
    }

    /**
     * Write to the pipe
     */
    public int write(ByteBuffer src) throws IOException {
        checkFlipped();
        return pipe.sink().write(src);
    }

    /**
     * Write to the pipe
     */
    public int write(byte[] src) throws IOException {
        checkFlipped();
        return write(ByteBuffer.wrap(src));
    }

    /**
     * Write to the pipe
     */
    public int write(byte[] src, int offset, int len) throws IOException {
        checkFlipped();
        return write(ByteBuffer.wrap(src, offset, len));
    }

    /**
     * True if this pipe is readable
     */
    public boolean isReadable() {
        return flipped;
    }

    /**
     * True if this pipe is writable
     */
    public boolean isWritable() {
        return !flipped;
    }

    /**
     * If the pipe is writable, this will close the input and switch to readable mode If the pipe is readable, this will
     * close the output and reset the pipe
     */
    public void close() throws IOException {
        if (!flipped) {
            if (pipe.sink().isOpen())
                pipe.sink().close();
            flipped = true;
        } else {
            if (pipe.source().isOpen())
                pipe.source().close();
            reset();
        }
    }

    /**
     * Reset the pipe. Switches the pipe to writable mode
     */
    public void reset() {
        try {
            if (pipe != null) {
                if (pipe.sink().isOpen())
                    pipe.sink().close();
                if (pipe.source().isOpen())
                    pipe.source().close();
            }
            pipe = Pipe.open();
            flipped = false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class PipeChannelInputStream extends FilterInputStream {
        private final Pipe pipe;

        protected PipeChannelInputStream(Pipe pipe, InputStream in) {
            super(in);
            this.pipe = pipe;
        }

        @Override
        public void close() throws IOException {
            pipe.source().close();
        }
    }

    private static class PipeChannelOutputStream extends FilterOutputStream {
        private final Pipe pipe;

        protected PipeChannelOutputStream(Pipe pipe, OutputStream in) {
            super(in);
            this.pipe = pipe;
        }

        @Override
        public void close() throws IOException {
            pipe.sink().close();
        }
    }

    private static class PipeChannelReader extends FilterReader {
        private final Pipe pipe;

        protected PipeChannelReader(Pipe pipe, Reader in) {
            super(in);
            this.pipe = pipe;
        }

        @Override
        public void close() throws IOException {
            pipe.source().close();
        }
    }

    private static class PipeChannelWriter extends FilterWriter {
        private final Pipe pipe;

        protected PipeChannelWriter(Pipe pipe, Writer in) {
            super(in);
            this.pipe = pipe;
        }

        @Override
        public void close() throws IOException {
            pipe.sink().close();
        }
    }

    public Appendable append(CharSequence csq) throws IOException {
        getWriter().append(csq);
        return this;
    }

    public Appendable append(char c) throws IOException {
        getWriter().append(c);
        return this;
    }

    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        getWriter().append(csq, start, end);
        return this;
    }

    public Appendable append(CharSequence csq, String charset) throws IOException {
        getWriter(charset).append(csq);
        return this;
    }

    public Appendable append(char c, String charset) throws IOException {
        getWriter(charset).append(c);
        return this;
    }

    public Appendable append(CharSequence csq, int start, int end, String charset) throws IOException {
        getWriter(charset).append(csq, start, end);
        return this;
    }

    public int read(CharBuffer cb) throws IOException {
        return getReader().read(cb);
    }

    public int read(CharBuffer cb, String charset) throws IOException {
        return getReader(charset).read(cb);
    }

    
}
