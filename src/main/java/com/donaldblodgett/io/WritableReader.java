/*
 * Copyright 2015 Donald Blodgett
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.donaldblodgett.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * This class facilitates writing to a reader. This is useful if working with
 * another reader that must have its result transformed before being read.
 * 
 * @author Donald Blodgett
 *
 */
public final class WritableReader extends Reader {
  private PipedReader reader;
  private Thread writeSide;

  private volatile Throwable exception;

  /**
   * Starts another thread that uses the <code>writeHandler</code> to write to
   * this reader.
   *
   * @param writeHandler
   *          Used to write to this reader.
   * @throws IllegalArgumentException
   *           if <code>writeHandler</code> is <code>null</code>
   * @throws IOException
   *           if this thread is interrupted or there was an error initializing
   *           the <code>writeHandler</code>
   */
  public WritableReader(WriteHandler writeHandler) throws IOException {
    if (writeHandler == null) {
      throw new IllegalArgumentException("writeHandler must not be null");
    }
    reader = new PipedReader();
    start(writeHandler, reader);
  }

  /**
   * Closes this reader and releases any system resources associated with the
   * stream. Also interrupts the <code>writeHandler</code> thread and blocks
   * until the <code>writeHandler</code> thread terminates.
   * 
   * @throws IOException
   *           if this thread is interrupted or an exception was thrown by the
   *           <code>writeHandler</code>
   */
  @Override
  public void close() throws IOException {
    writeSide.interrupt();
    try {
      writeSide.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      IOException ioe = new InterruptedIOException("Interrupted while waiting for writeHandler to finish");
      throw supressWriterException(ioe);
    } finally {
      throwIfWriterExceptionExists();
      reader.close();
    }
  }

  /**
   * Reads the next character of data from this reader. If no character is
   * available because the end of the stream has been reached, the value -1 is
   * returned. This method blocks until input data is available, the end of the
   * stream is detected, or an exception is thrown.
   * 
   * @return the next character of data, or -1 if the end of the stream is
   *         reached.
   * @throws IOException
   *           if this thread is interrupted or an exception was thrown by the
   *           <code>writeHandler</code>
   */
  @Override
  public int read() throws IOException {
    throwIfWriterExceptionExists();
    try {
      return reader.read();
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Reads characters into an array. This method will block until some input is
   * available, an I/O error occurs, or the end of the stream is reached.
   * 
   * @param cbuf
   *          the destination buffer.
   * @return The number of characters read, or -1 if the end of the stream has
   *         been reached
   * @throws IOException
   *           if the stream is closed, or this thread is interrupted or an
   *           exception was thrown by the <code>writeHandler</code>
   */
  @Override
  public int read(char[] cbuf) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return reader.read(cbuf);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Reads up to <code>len</code> characters of data from this stream into an
   * array of characters. Less than <code>len</code> characters will be read if
   * the end of the data stream is reached or if <code>len</code> exceeds the
   * buffer size. This method blocks until at least one character of input is
   * available.
   * 
   * @param cbuf
   *          the destination buffer.
   * @param off
   *          start offset of the data.
   * @param len
   *          the maximum number of characters read.
   * @return The number of characters read, or -1 if the end of the stream has
   *         been reached
   * @throws IOException
   *           if the stream is closed, or this thread is interrupted or an
   *           exception was thrown by the <code>writeHandler</code>
   */
  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return reader.read(cbuf, off, len);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Attempts to read characters into the specified character buffer. The buffer
   * is used as a repository of characters as-is: the only changes made are the
   * results of a put operation. No flipping or rewinding of the buffer is
   * performed.
   * 
   * @param target
   *          the buffer to read characters into
   * @return The number of characters added to the buffer, or -1 if this source
   *         of characters is at its end
   * @throws IOException
   *           if the stream is closed, or this thread is interrupted or an
   *           exception was thrown by the <code>writeHandler</code>
   */
  @Override
  public int read(CharBuffer target) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return reader.read(target);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Tell whether this stream is ready to be read. This stream is ready if the
   * circular buffer is not empty.
   * 
   * @return <code>true</code> if the next
   *         <code>read()<code> is guaranteed not to block for
   *         input, <code>false</code> otherwise. Note that returning false does
   *         not guarantee that the next read will block.
   * @throws IOException
   *           if the stream is closed, or this thread is interrupted or an
   *           exception was thrown by the <code>writeHandler</code>
   */
  @Override
  public boolean ready() throws IOException {
    throwIfWriterExceptionExists();
    return reader.ready();
  }

  /**
   * Skips over and discards <code>n</code> characters of data from this stream.
   * The <code>skip</code> method may, for a variety of reasons, end up skipping
   * over some smaller number of characters, possibly 0. This may result from
   * any of a number of conditions; reaching end of stream before <code>n</code>
   * characters have been skipped is only one possibility. The actual number of
   * characters skipped is returned. If <code>n</code> is negative, 0 is
   * returned and no characters are skipped.
   * 
   * @param n
   *          the number of characters to be skipped.
   * @return the actual number of characters skipped.
   * @throws IOException
   *           if the stream is closed, or this thread is interrupted or
   *           an exception was thrown by the <code>outputHandler</code>
   */
  @Override
  public long skip(long n) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return reader.skip(n);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  private void start(final WriteHandler writeHandler, final PipedReader reader) throws IOException {
    final CountDownLatch connected = new CountDownLatch(1);
    writeSide = new Thread() {
      @Override
      public void run() {
        try (PipedWriter writer = new PipedWriter(reader)) {
          connected.countDown();
          Writer proxyWriter = new FilterWriter(writer) {
            @Override
            public void close() throws IOException {
              throw new IllegalStateException("writer must not be closed from writeHandler");
            }
          };
          writeHandler.write(proxyWriter);
        } catch (Throwable e) {
          exception = e;
        }
      }
    };
    writeSide.start();
    try {
      connected.await();
    } catch (InterruptedException e) {
      writeSide.interrupt();
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("Interrupted while waiting for writerHandler to initialize");
    }
  }

  private void throwIfWriterExceptionExists() throws IOException {
    if (exception != null) {
      IOException ioe = new IOException(exception.getMessage(), exception);
      exception = null;
      throw ioe;
    }
  }

  private IOException supressWriterException(IOException readException) throws IOException {
    if (exception != null) {
      readException.addSuppressed(exception);
      exception = null;
    }
    return readException;
  }
}
