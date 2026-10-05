package org.xrose.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class NativeUtils {
   private static File extractedDir;

   public static synchronized void extractNativeLibrary(String libName) {
      if (extractedDir == null) {
         try {
            extractedDir = new File(System.getProperty("java.io.tmpdir"), "xrose-natives");
            if (!extractedDir.exists()) {
               extractedDir.mkdirs();
            }

            String arch = System.getProperty("os.arch").contains("64") ? "win32-x86-64" : "win32-x86";
            String dllPath = "/" + arch + "/" + libName + ".dll";
            File target = new File(extractedDir, libName + ".dll");
            label66:
            if (!target.exists()) {
               try (InputStream in = NativeUtils.class.getResourceAsStream(dllPath)) {
                  if (in != null) {
                     try (OutputStream out = new FileOutputStream(target)) {
                        byte[] buf = new byte[8192];

                        int len;
                        while ((len = in.read(buf)) != -1) {
                           out.write(buf, 0, len);
                        }
                        break label66;
                     }
                  }

                  System.err.println("[NativeUtils] Native DLL not found in resources: " + dllPath);
               }

               return;
            }

            System.setProperty("jna.library.path", extractedDir.getAbsolutePath());
         } catch (Exception e) {
            System.err.println("[NativeUtils] Failed to extract native library: " + e.getMessage());
            e.printStackTrace();
         }
      }
   }

   private NativeUtils() {
   }
}

