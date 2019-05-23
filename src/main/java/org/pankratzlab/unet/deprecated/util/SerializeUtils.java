/*-
 * #%L
 * DonorCheck
 * %%
 * Copyright (C) 2018 - 2019 Computational Pathology - University of Minnesota
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.pankratzlab.unet.deprecated.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/** Utility class for working with {@link Serializable} classes. */
public final class SerializeUtils {
  // TODO combine with SerializedFiles in Genvisis

  private SerializeUtils() {
    // Prevent instantiation of static utility class
  }

  public static <T extends Serializable> T read(String path, Class<T> dest) {
    File f = new File(path);
    if (f.exists()) {
      try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(f));
          ObjectInputStream in = new ObjectInputStream(fileIn)) {
        T cached = dest.cast(in.readObject());
        return cached;
      } catch (IOException exc) {
        exc.printStackTrace();
      } catch (ClassNotFoundException exc) {
        exc.printStackTrace();
      } catch (ClassCastException exc) {
        exc.printStackTrace();
      }
    }
    return null;
  }

  public static void write(Object o, String path) throws IOException {
    new File(path).getParentFile().mkdirs();
    try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(path));
        ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
      out.writeObject(o);
    }
  }
}
