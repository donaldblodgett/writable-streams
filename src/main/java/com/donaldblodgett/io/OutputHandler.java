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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This interface is used by {@link OutputableInputStream} to output to the
 * input stream.
 * 
 * @author Donald Blodgett
 *
 */
public interface OutputHandler {
  /**
   * The write method is invoked by the {@link OutputableInputStream} in a
   * separate thread, once. The {@link OutputStream} supplied is not thread safe
   * and must be written to from the thread provided by the
   * {@link OutputableInputStream}. The provided {@link OutputStream} will be
   * closed after this method returns.
   * 
   * @param output
   *          the output stream that must be used to write to the
   *          {@link OutputableInputStream}
   * @throws IOException
   *           if an I/O exception occurs
   */
  void write(OutputStream output) throws IOException;
}
