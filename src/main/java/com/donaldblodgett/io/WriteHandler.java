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
import java.io.Writer;

/**
 * This interface is used by {@link WritableReader} to write to the reader.
 * 
 * @author Donald Blodgett
 *
 */
public interface WriteHandler {
  /**
   * The write method is invoked by the {@link WritableReader} in a separate
   * thread, once. The {@link Writer} supplied is not thread safe and must be
   * written to from the thread provided by the {@link WritableReader}. The
   * provided {@link Writer} will be closed after this method returns.
   * 
   * @param writer
   *          the writer that must be used to write to the
   *          {@link WritableReader}
   * @throws IOException
   *           if an I/O exception occurs
   */
  void write(Writer writer) throws IOException;
}
