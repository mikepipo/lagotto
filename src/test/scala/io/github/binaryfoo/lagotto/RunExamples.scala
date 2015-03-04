package io.github.binaryfoo.lagotto

import java.io.{ByteArrayOutputStream, File, FileWriter, PrintWriter}
import java.nio.file.{Files, StandardCopyOption}

import io.github.binaryfoo.lagotto.shell.Main
import org.asciidoctor.internal.JRubyAsciidoctor
import org.asciidoctor.{OptionsBuilder, SafeMode}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object RunExamples {

  val Example = "    LAGO: (.*)".r

  def main(args: Array[String]) {
    val outputFile = new File("docs/examples.adoc")
    val out = new PrintWriter(new FileWriter(outputFile))
    Source.fromFile("src/docs/examples.adoc").getLines().foreach {
      case Example(arguments) =>
        out.println("    lago " + arguments + "\n")
        out.println(indent(lagoOutputFrom(cleanAndSplit(arguments))))
      case line =>
        out.println(line)
    }
    out.close()
    move("rtts.csv")
    move("rtts.gp")
    val asciiDoctor = JRubyAsciidoctor.create()
    asciiDoctor.renderFile(outputFile, OptionsBuilder.options().safe(SafeMode.UNSAFE))
    asciiDoctor.renderFile(new File("docs/reference.adoc"), OptionsBuilder.options().safe(SafeMode.UNSAFE))
  }

  def move(file: String): Unit = {
    Files.move(new File(file).toPath, new File(s"docs/$file").toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  def cleanAndSplit(arguments: String): Array[String] = {
    var quoted = false
    val current = new StringBuilder
    val args = new ArrayBuffer[String]()
    arguments.foreach {
      case '\'' => quoted = !quoted
      case x if quoted => current.append(x)
      case ' ' =>
        if (current.nonEmpty) {
          args += current.toString()
          current.delete(0, current.length)
        }
      case x => current.append(x)
    }
    if (current.nonEmpty)
      args += current.toString()
    args.toArray
  }

  def lagoOutputFrom(args: Array[String]): String = {
    println(s"Running ${args.mkString(" ")}")
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(args.toArray)
    }
    out.toString
  }

  def indent(s: String): String = {
    Source.fromString(s).getLines().map("    " + _).mkString("\n")
  }

}
