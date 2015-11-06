package com.donaldblodgett.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.nio.CharBuffer;
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
public class WritableReaderTest {
  @After
  public void tearDown() {
    // Ensure the test thread is not interrupted
    Thread.interrupted();
  }

  @SuppressWarnings("resource")
  @Test(expected = IllegalArgumentException.class)
  public void initExpectIllegalArgument() throws IOException {
    new WritableReader(null);
  }

  @Test(expected = InterruptedIOException.class)
  public void initInterruptedThread() throws IOException {
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(new char[2048]);
      }
    };
    Thread.currentThread().interrupt();
    try (WritableReader input = new WritableReader(writeHandler)) {
    } catch (InterruptedIOException e) {
      // Make sure to clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void closeWithUnfinishedWriter() throws IOException, InterruptedException, BrokenBarrierException {
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(new char[2048]);
      }
    };
    WritableReader input = new WritableReader(writeHandler);
    input.close();
  }

  @Test
  public void readWithSuccessfulWriteHandler() throws IOException {
    Random random = new Random();
    final int randomNumber = random.nextInt(256);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(randomNumber);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      assertEquals(input.read(), randomNumber);
    }
  }

  @Test(expected = IOException.class)
  public void readExpectNonIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read();
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readExpectIOExceptionFromWriteHandler() throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
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
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.close();
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      input.read();
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.write(new char[2048]);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
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
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
      }
    };
    WritableReader input = new WritableReader(writeHandler);
    input.close();
    input.read();
  }

  public String generateString() {
    return RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(256, 2048));
  }

  public char[] generateChars() {
    return generateString().toCharArray();
  }

  @Test
  public void readCharsWithSuccessfulWriteHandler() throws IOException {
    final char[] randomChars = generateChars();
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(randomChars);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      CharBuffer allChars = CharBuffer.allocate(randomChars.length);
      char[] chars = new char[256];
      for (int i = input.read(chars); i > -1; i = input.read(chars)) {
        allChars.put(Arrays.copyOf(chars, i));
      }
      assertArrayEquals(allChars.array(), randomChars);
    }
  }

  @Test(expected = IOException.class)
  public void readCharsExpectNonIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read(new char[256]);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readCharsExpectIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read(new char[256]);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readCharsWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.close();
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      input.read(new char[256]);
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readCharsOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.write(new char[2048]);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.read(new char[256]);
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void readCharsAfterClose() throws IOException {
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
      }
    };
    WritableReader input = new WritableReader(writeHandler);
    input.close();
    input.read(new char[256]);
  }

  @Test
  public void readIntoWithSuccessfulWriteHandler() throws IOException {
    final char[] randomChars = generateChars();
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(randomChars, 0, randomChars.length);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      char[] allChars = new char[randomChars.length];
      for (int i = input.read(allChars, 0, allChars.length); i > -1; i = input.read(allChars, i, allChars.length - i))
        ;
      assertArrayEquals(allChars, randomChars);
    }
  }

  @Test(expected = IOException.class)
  public void readIntoExpectNonIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read(new char[256], 0, 256);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readIntoExpectIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read(new char[256], 0, 256);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readIntoWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.close();
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      input.read(new char[256], 0, 256);
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readIntoOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.write(new char[2048]);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.read(new char[256], 0, 256);
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void readIntoAfterClose() throws IOException {
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
      }
    };
    WritableReader input = new WritableReader(writeHandler);
    input.close();
    input.read(new char[256], 0, 256);
  }

  @Test(expected = IOException.class)
  public void readIntoWriteThenException() throws IOException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          Thread.currentThread().wait(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        throw new IOException(randomMessage);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      input.read(new char[256], 0, 256);
    }
  }

  @Test
  public void readIntoBufferWithSuccessfulWriteHandler() throws IOException {
    final String randomString = generateString();
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(randomString);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      CharBuffer buffer = CharBuffer.allocate(randomString.length());
      StringBuilder sb = new StringBuilder();
      for (int i = input.read(buffer); i > -1; i = input.read(buffer)) {
        buffer.flip();
        sb.append(buffer,0,i);
      }
      assertEquals(sb.toString(), randomString);
    }
  }

  @Test(expected = IOException.class)
  public void readIntoBufferExpectNonIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read(CharBuffer.allocate(256));
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readIntoBufferExpectIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.read(CharBuffer.allocate(256));
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void readIntoBufferWithWriteHandlerClosingWriter() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.close();
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      input.read(CharBuffer.allocate(256));
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void readIntoBufferOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.write(new char[2048]);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      Thread.currentThread().interrupt();
      input.read(CharBuffer.allocate(256));
    } catch (InterruptedIOException e) {
      // Make sure the clear the interrupted flag so other test are not
      // interrupted
      assertEquals(Thread.interrupted(), true);
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void readIntoBufferAfterClose() throws IOException {
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
      }
    };
    WritableReader input = new WritableReader(writeHandler);
    input.close();
    input.read(CharBuffer.allocate(256));
  }

  @Test(expected = IOException.class)
  public void readIntoBufferWriteThenException() throws IOException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          Thread.currentThread().wait(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        throw new IOException(randomMessage);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      input.read(CharBuffer.allocate(256));
    }
  }

  @Test
  public void readyAfterWrite() throws IOException, InterruptedException {
    Random random = new Random();
    final int randomNumber = random.nextInt(256);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(new char[randomNumber]);
        latch.countDown();
      }
    };
    try (WritableReader reader = new WritableReader(writeHandler)) {
      latch.await();
      assertEquals(reader.ready(), true);
    }
  }

  @Test
  public void skipWithSuccessfulWriteHandler() throws IOException {
    final char[] randomChars = generateChars();
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        writer.write(randomChars, 0, randomChars.length);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      int skip = RandomUtils.nextInt(50, 100);
      char[] allChars = new char[randomChars.length - skip];
      input.skip(skip);
      for (int i = input.read(allChars, 0, allChars.length); i > -1; i = input.read(allChars, i, allChars.length - i))
        ;
      assertArrayEquals(Arrays.copyOfRange(randomChars, skip, randomChars.length), allChars);
    }
  }

  @Test(expected = IOException.class)
  public void skipExpectNonIOExceptionFromWriteHandler()
      throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new RuntimeException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.await();
      input.skip(100);
    } catch (IOException e) {
      assertEquals(randomMessage, e.getMessage());
      throw new IOException();
    }
  }

  @Test(expected = IOException.class)
  public void skipExpectIOExceptionFromWriteHandler() throws IOException, InterruptedException, BrokenBarrierException {
    final String randomMessage = RandomStringUtils.randomAlphanumeric(10);
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          throw new IOException(randomMessage);
        } finally {
          latch.countDown();
        }
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
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
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.close();
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
      latch.countDown();
      input.skip(100);
    }
  }

  @Test(expected = InterruptedIOException.class)
  public void skipOnInterruptedThread() throws IOException {
    final CountDownLatch latch = new CountDownLatch(1);
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        writer.write(new char[2048]);
      }
    };
    try (WritableReader input = new WritableReader(writeHandler)) {
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
    WriteHandler writeHandler = new WriteHandler() {
      @Override
      public void write(Writer writer) throws IOException {
      }
    };
    WritableReader input = new WritableReader(writeHandler);
    input.close();
    input.skip(100);
  }
}
