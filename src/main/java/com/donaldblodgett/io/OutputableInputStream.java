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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * This class facilitates outputing to an input stream. This is useful if
 * working with another input stream that must have its result transformed
 * before being read.
 * 
 * @author Donald Blodgett
 *
 */
public final class OutputableInputStream extends InputStream {
  private PipedInputStream input;
  private Thread writeSide;

  private volatile Throwable exception;

  /**
   * Starts another thread that uses the <code>outputHandler</code> to write to
   * this input stream.
   *
   * @param outputHandler
   *          Used to write to this input stream.
   * @throws IllegalArgumentException
   *           if <code>outputHandler</code> is <code>null</code>
   * @throws IOException
   *           if this thread is interrupted or there was an error initializing
   *           the <code>outputHandler</code>
   */
  public OutputableInputStream(OutputHandler outputHandler) throws IOException {
    if (outputHandler == null) {
      throw new IllegalArgumentException("outputHandler must not be null");
    }
    input = new PipedInputStream();
    start(outputHandler, input);
  }

  /**
   * Returns the number of bytes that can be read from this input stream without
   * blocking.
   * 
   * @return the number of bytes that can be read from this input stream without
   *         blocking, or 0 if this input stream has been closed by invoking its
   *         close() method.
   * @throws IOException
   *           if an I/O error occurs
   */
  @Override
  public int available() throws IOException {
    throwIfWriterExceptionExists();
    return input.available();
  }

  /**
   * Closes this input stream and releases any system resources associated with
   * the stream. Also interrupts the <code>outputHandler</code> thread and
   * blocks until the <code>outputHandler</code> thread terminates.
   * 
   * @throws IOException
   *           if this thread is interrupted or an exception was thrown by the
   *           <code>outputHandler</code>
   */
  @Override
  public void close() throws IOException {
    writeSide.interrupt();
    try {
      writeSide.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      IOException ioe = new InterruptedIOException("Interrupted while waiting for outputHandler to finish");
      throw supressWriterException(ioe);
    } finally {
      throwIfWriterExceptionExists();
      input.close();
    }
  }

  /**
   * Reads the next byte of data from this input stream. The value byte is
   * returned as an int in the range 0 to 255. This method blocks until input
   * data is available, the end of the stream is detected, or an exception is
   * thrown.
   * 
   * @return the next byte of data, or -1 if the end of the stream is reached.
   * @throws IOException
   *           if this thread is interrupted or an exception was thrown by the
   *           <code>outputHandler</code>
   */
  @Override
  public int read() throws IOException {
    throwIfWriterExceptionExists();
    try {
      return input.read();
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Reads some number of bytes from the input stream and stores them into the
   * buffer array <code>buf</code>. The number of bytes actually read is
   * returned as an integer. This method blocks until input data is available,
   * end of stream is detected, or an exception is thrown.
   * 
   * If the length of <code>buf</code> is zero, then no bytes are read and 0 is
   * returned; otherwise, there is an attempt to read at least one byte. If no
   * byte is available because the stream has ended, the value -1 is returned;
   * otherwise, at least one byte is read and stored into <code>buf</code>.
   * 
   * The first byte read is stored into element <code>buf[0]</code>, the next
   * one into <code>buf[0]</code>, and so on. The number of bytes read is, at
   * most, equal to the length of <code>buf</code>. Let <code>k</code> be the
   * number of bytes actually read; these bytes will be stored in elements
   * <code>buf[0]</code> through <code>buf[k-1]</code>, leaving elements
   * <code>buf[k]</code> through <code>buf[buf.length-1]</code> unaffected.
   * 
   * @param buf
   *          the buffer into which the data is read.
   * @return the next byte of data, or -1 if the end of the stream is reached.
   * @throws IOException
   *           if the input stream is closed, or this thread is interrupted or
   *           an exception was thrown by the <code>outputHandler</code>
   */
  @Override
  public int read(byte[] buf) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return input.read(buf);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Reads up to <code>len</code> bytes of data from this input stream into an
   * array of bytes. Less than <code>len</code> bytes will be read if the end of
   * the data stream is reached or if <code>len</code> exceeds the pipe's buffer
   * size. If <code>len</code> is zero, then no bytes are read and 0 is
   * returned; otherwise, the method blocks until at least 1 byte of input is
   * available, end of the stream has been detected, or an exception is thrown.
   * 
   * @param buf
   *          the buffer into which the data is read.
   * @param off
   *          the start offset in array b at which the data is written.
   * @param len
   *          the maximum number of bytes to read.
   * @return the total number of bytes read into the buffer, or -1 if there is
   *         no more data because the end of the stream has been reached.
   * @throws NullPointerException
   *           if <code>buf</code> is null.
   * @throws IndexOutOfBoundsException
   *           if <code>off</code> is negative, <code>len</code> is negative, or
   *           <code>len</code> is greater than <code>b.length - off</code>
   * @throws IOException
   *           if the input stream is closed, or this thread is interrupted or
   *           an exception was thrown by the <code>outputHandler</code>
   */
  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return input.read(buf, off, len);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  /**
   * Skips over and discards <code>n</code> bytes of data from this input
   * stream. The <code>skip</code> method may, for a variety of reasons, end up
   * skipping over some smaller number of bytes, possibly 0. This may result
   * from any of a number of conditions; reaching end of stream before
   * <code>n</code> bytes have been skipped is only one possibility. The actual
   * number of bytes skipped is returned. If <code>n</code> is negative, 0 is
   * returned and no bytes are skipped.
   * 
   * @param n
   *          the number of bytes to be skipped.
   * @return the actual number of bytes skipped.
   * @throws IOException
   *           if the input stream is closed, or this thread is interrupted or
   *           an exception was thrown by the <code>outputHandler</code>
   */
  @Override
  public long skip(long n) throws IOException {
    throwIfWriterExceptionExists();
    try {
      return input.skip(n);
    } catch (InterruptedIOException e) {
      Thread.currentThread().interrupt();
      throw supressWriterException(e);
    } catch (IOException e) {
      throw supressWriterException(e);
    }
  }

  private void start(final OutputHandler outputHandler, final PipedInputStream input) throws IOException {
    final CountDownLatch connected = new CountDownLatch(1);
    writeSide = new Thread() {
      @Override
      public void run() {
        try (PipedOutputStream output = new PipedOutputStream(input)) {
          connected.countDown();
          OutputStream proxyOutput = new FilterOutputStream(output) {
            @Override
            public void write(byte[] b) throws IOException {
              out.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
              out.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
              throw new IllegalStateException("writer must not be closed from outputHandler");
            }
          };
          outputHandler.write(proxyOutput);
        } catch (Throwable e) {
          exception = e;
        } finally {
          connected.countDown();
        }
      }
    };
    writeSide.start();
    try {
      connected.await();
    } catch (InterruptedException e) {
      writeSide.interrupt();
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("Interrupted while waiting for outputHandler to initialize");
    }
    throwIfWriterExceptionExists();
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
