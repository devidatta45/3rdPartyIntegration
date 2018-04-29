package com.sony.utils

import scala.io.Source
import scala.util.matching.Regex

trait FileConfig extends XmlLoader {
  def getPattern(str: String): String = {
    if (str.contains("<Product ")) {
      val pattern: Regex = """<Product .+<\/Product>""".r
      val product = pattern.findFirstIn(str)
      if (product.isDefined) product.get else ""
    }
    else {
      ""
    }
  }

  def getProducts(): List[String] = {
    val list = EndLine.getProductWithEndLine
    val lines = sourceFile.getLines()
    var from = 0

    list map { len =>
      var wholeString = lines.take(len - from).mkString
      from = len
      if (wholeString.startsWith("</Product>") || wholeString.startsWith(" </Product>")) {
        wholeString = wholeString.replace("</Product>", "")
      }
      if (!wholeString.endsWith("</Product>")) {
        wholeString = wholeString + "</Product>"
      }
      getPattern(wholeString)
    }
  }

  def getProducts(list: List[Int], file: String): List[String] = {
    var from = 0
    val lines = Source.fromFile(file).getLines()
    list map { len =>
      var wholeString = lines.take(len - from).mkString
      from = len
      if (wholeString.startsWith("</Product>") || wholeString.startsWith(" </Product>")) {
        wholeString = wholeString.replace("</Product>", "")
      }
      if (!wholeString.endsWith("</Product>")) {
        wholeString = wholeString + "</Product>"
      }
      getPattern(wholeString)
    }
  }

}
