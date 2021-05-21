package org.errors4s.sbt

import java.io.File
import java.net.URL

object ScalaApiDoc {

  def jreModuleLink(jreVersion: String)(module: String): (File, URL) =
    new File(s"/module/${module}") ->
      new URL(s"https://docs.oracle.com/en/java/javase/${jreVersion}/docs/api/${module}")
}
