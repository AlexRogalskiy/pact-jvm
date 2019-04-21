package au.com.dius.pact.matchers

/**
 * Interface to a mismatch
 */
interface Mismatch {
  fun description(): String
}

/**
 * Interface to a factory class to create a mismatch
 *
 * @param <Mismatch> Type of mismatch to create
 */
interface MismatchFactory<out M : Mismatch> {
  fun create(expected: Any?, actual: Any?, message: String, path: List<String>): M
}

data class HeaderMismatch(val headerKey: String, val expected: String, val actual: String, val mismatch: String? = null) : Mismatch {
  override fun description(): String = if (mismatch != null) {
    "HeaderMismatch - $mismatch"
  } else {
    toString()
  }

  fun merge(mismatch: HeaderMismatch): HeaderMismatch {
    return if (this.mismatch != null) {
      copy(mismatch = this.mismatch + ", " + mismatch.mismatch)
    } else {
      copy(mismatch = mismatch.mismatch)
    }
  }
}

object HeaderMismatchFactory : MismatchFactory<HeaderMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    HeaderMismatch(path.last(), expected.toString(), actual.toString(), message)
}

data class BodyMismatch @JvmOverloads constructor(
  val expected: Any?,
  val actual: Any?,
  val mismatch: String? = null,
  val path: String = "/",
  val diff: String? = null
)
  : Mismatch {
  override fun description(): String = if (mismatch != null) {
    "BodyMismatch - $mismatch"
  } else {
    toString()
  }
}

object BodyMismatchFactory : MismatchFactory<BodyMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
  BodyMismatch(expected, actual, message, path.joinToString("."))
}

data class MetadataMismatch(val key: String, val expected: Any?, val actual: Any?, val mismatch: String) : Mismatch {
  override fun description() = mismatch

  fun merge(mismatch: MetadataMismatch) = copy(mismatch = this.mismatch + ", " + mismatch.mismatch)
}

object MetadataMismatchFactory : MismatchFactory<MetadataMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    MetadataMismatch(path.last(), expected, actual, message)
}
