package models

case class RankIndexData(
  url: String,
  annualisedValueInGBP: Double,
  acquisitions: Int,
  pageviews: Long,
  headline: Option[String],
  annualisedValueInGBPPer1000: Double,
  acquisitionsPer1000: Double
)
