package com.donaldblodgett.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class OutputableInputStreamTest {
  @After
  public void tearDown() {
    // Ensure the test thread is not interrupted
    Thread.interrupted();
  }

  @SuppressWarnings("resource")
  @Test(expected = IllegalArgumentException.class)
  public void initExpectIllegalArgument() throws IOException {
    new OutputableInputStream(null);
  }

  @Test(expected = InterruptedIOException.class)
  public void initInterruptedThread() throws IOException {
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(new byte[2048]);
      }
    };
    Thread.currentThread().interrupt();
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
    } catch (InterruptedIOException e) {
      // Make sure to clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test
  public void availableAfterWrite() throws IOException, InterruptedException {
    Random random = new Random();
    final int randomNumber = random.nextInt(256);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(new byte[randomNumber]);
        latch.countDown();
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      assertEquals(input.available(), randomNumber);
    }
  }

  @Test(expected = IOException.class)
  public void closeWithUnfinishedWriter() throws IOException, InterruptedException, BrokenBarrierException {
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(new byte[2048]);
      }
    };
    OutputableInputStream input = new OutputableInputStream(outputHandler);
    input.close();
  }

  @Test
  public void readWithSuccessfulOutputHandler() throws IOException {
    Random random = new Random();
    final int randomNumber = random.nextInt(256);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(randomNumber);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      assertEquals(input.read(), randomNumber);
    }
  }

  @Test(expected = IOException.class)
  public void readExpectNonIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.read();
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readExpectIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.read();
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.close();
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      input.read();
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.write(new byte[2048]);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.read();
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void readAfterClose() throws IOException {
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
      }
    };
    OutputableInputStream input = new OutputableInputStream(outputHandler);
    input.close();
    input.read();
  }

  public byte[] generateBytes() {
    return RandomUtils.nextBytes(RandomUtils.nextInt(256, 2048));
  }

  @Test
  public void readBytesWithSuccessfulOutputHandler() throws IOException {
    final byte[] randomBytes = generateBytes();
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(randomBytes);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      ByteBuffer allBytes = ByteBuffer.allocate(randomBytes.length);
      byte[] bytes = new byte[256];
      for (int i = input.read(bytes); i > -1; i = input.read(bytes)) {
        allBytes.put(Arrays.copyOf(bytes, i));
      }
      assertArrayEquals(allBytes.array(), randomBytes);
    }
  }

  @Test(expected = IOException.class)
  public void readBytesExpectNonIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.read(new byte[256]);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readBytesExpectIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.read(new byte[256]);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readBytesWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.close();
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      input.read(new byte[256]);
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readBytesOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.write(new byte[2048]);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.read(new byte[256]);
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void readBytesAfterClose() throws IOException {
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
      }
    };
    OutputableInputStream input = new OutputableInputStream(outputHandler);
    input.close();
    input.read(new byte[256]);
  }

  @Test
  public void readIntoWithSuccessfulOutputHandler() throws IOException {
    final byte[] randomBytes = generateBytes();
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(randomBytes, 0, randomBytes.length);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      byte[] allBytes = new byte[randomBytes.length];
      for (int i = input.read(allBytes, 0, allBytes.length); i > -1; i = input.read(allBytes, i, allBytes.length - i))
        ;
      assertArrayEquals(allBytes, randomBytes);
    }
  }

  @Test(expected = IOException.class)
  public void readIntoExpectNonIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.read(new byte[256], 0, 256);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readIntoExpectIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.read(new byte[256], 0, 256);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readIntoWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.close();
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      input.read(new byte[256], 0, 256);
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readIntoOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.write(new byte[2048]);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.read(new byte[256], 0, 256);
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void readIntoAfterClose() throws IOException {
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
      }
    };
    OutputableInputStream input = new OutputableInputStream(outputHandler);
    input.close();
    input.read(new byte[256], 0, 256);
  }

  @Test(expected = IOException.class)
  public void readIntoWriteThenException() throws IOException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          Thread.currentThread().wait(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        throw new IOException(randomMessage);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      input.read(new byte[256], 0, 256);
    }
  }

  @Test
  public void skipWithSuccessfulOutputHandler() throws IOException {
    final byte[] randomBytes = generateBytes();
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        output.write(randomBytes, 0, randomBytes.length);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      int skip = RandomUtils.nextInt(50, 100);
      byte[] allBytes = new byte[randomBytes.length - skip];
      input.skip(skip);
      for (int i = input.read(allBytes, 0, allBytes.length); i > -1; i = input.read(allBytes, i, allBytes.length - i))
        ;
      assertArrayEquals(Arrays.copyOfRange(randomBytes, skip, randomBytes.length), allBytes);
    }
  }

  @Test(expected = IOException.class)
  public void skipExpectNonIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.skip(100);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void skipExpectIOExceptionFromOutputHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.await();
      input.skip(100);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void skipWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.close();
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      input.skip(100);
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void skipOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        output.write(new byte[2048]);
      }
    };
    try (OutputableInputStream input = new OutputableInputStream(outputHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.skip(100);
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void skipAfterClose() throws IOException {
    OutputHandler outputHandler = new OutputHandler() {
      @Override
      public void write(OutputStream output) throws IOException {
      }
    };
    OutputableInputStream input = new OutputableInputStream(outputHandler);
    input.close();
    input.skip(100);
  }
}
