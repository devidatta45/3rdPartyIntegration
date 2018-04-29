package com.sony.services

import com.sony.repository.SourceService
import com.sony.utils.{FileConfig, MockProduct, RestService}
import org.json4s.JValue
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import play.api.libs.ws.WSRequest
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object BVApiService extends RestService with SourceService {
  override def getResult: Future[List[String]] = {
    val productList = MockProduct.productList
    val result = productList map { product =>
      val url = s"http://stg.api.bazaarvoice.com/data/reviews.json?apiversion=5.4&" +
        s"passkey=${product.passkey}&" +
        s"Filter=ProductId:${product.id}&locale=${product.locale}&Filter=ContentLocale:${product.locale}&Limit=100" +
        s"&Include=Products&Stats=ReviewsOffset=0"
      val request: WSRequest = client.url(url)
      callRestService(request, product.id)
    }
    Future.sequence(result)
  }

  def callRestService(request: WSRequest, productId: String): Future[String] = {
    for {
      result <- request.get()
      finalResponse = parse(result.body)
      lastResponse = changeFormat(finalResponse, productId)
      finalProduct = compact(render(lastResponse))
    } yield finalProduct
  }

  def changeFormat(value: JValue, productId: String): JValue = {
    val product = value \ "Includes" \ "Products" \ productId
    val reviewStatistics = product \ "ReviewStatistics"
    val reviews = value \\ "Results"
    val finalProduct = product merge {
      JObject("Reviews" -> reviews)
    } merge {
      JObject("NativeReviewStatistics" -> reviewStatistics)
    }
    finalProduct
  }
}

object XmlApiService extends SourceService with FileConfig {
  override def getResult: Future[List[String]] = Future {
    getProducts()
  }

  override def file: String = "./masterData/bv_sony-global_standard_client_feed.xml"
}
