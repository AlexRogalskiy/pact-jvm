package au.com.dius.pact.model

import au.com.dius.pact.matchers.Mismatch

sealed trait ResponseMatch
case object FullResponseMatch extends ResponseMatch
case class ResponseMismatch(mismatches: Seq[Mismatch]) extends ResponseMatch

object ResponseMatching extends ResponseMatching(true)

class ResponseMatching(val allowUnexpectedKeys: Boolean) {
  import au.com.dius.pact.model.Matching._
  import scala.collection.JavaConverters._

  def matchRules(expected: Response, actual: Response): ResponseMatch = {
    val mismatches = responseMismatches(expected, actual)
    if (mismatches.isEmpty) FullResponseMatch
    else ResponseMismatch(mismatches)
  }
  
  def responseMismatches(expected: Response, actual: Response): Seq[Mismatch] = {
    (matchStatus(expected.getStatus, actual.getStatus)
      ++ au.com.dius.pact.matchers.Matching.matchHeaders(expected, actual).asScala
      ++ matchBody(expected, actual, allowUnexpectedKeys)).toSeq
  }
}
