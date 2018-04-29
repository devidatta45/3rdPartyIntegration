package com.sony.models

import com.sony.utils.BaseEntity

case class ProductWithReview(override val _id: String = "", id: String, name: String, description: String) extends BaseEntity
