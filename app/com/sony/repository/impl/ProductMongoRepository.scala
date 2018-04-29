package com.sony.repository.impl

import com.sony.models.ProductWithReview
import com.sony.utils.{BaseFileRepository, BaseMongoRepository}

class ProductMongoRepository extends BaseMongoRepository[ProductWithReview] {
  override def table: String = "Product"
}

object ProductMongoRepositoryImpl extends ProductMongoRepository

class ProductFileRepository extends BaseFileRepository[ProductWithReview]

object ProductFileRepositoryImpl extends ProductFileRepository