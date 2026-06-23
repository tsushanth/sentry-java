package io.sentry.android.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.sentry.ISpan
import io.sentry.NoOpSpan
import io.sentry.SentryNanotimeDate
import io.sentry.SpanStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ExtendedAppStartSpanTest {
  @Test
  fun `getStartDate returns the start date passed to the constructor`() {
    val startDate = SentryNanotimeDate()
    val span = ExtendedAppStartSpan(startDate)
    assertSame(startDate, span.startDate)
  }

  @Test
  fun `before materialize, startChild returns a NoOpSpan`() {
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    assertSame(NoOpSpan.getInstance(), span.startChild("op"))
  }

  @Test
  fun `after materialize, calls delegate to the real child`() {
    val child = mock<ISpan>()
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    span.materialize(child)

    span.startChild("op")
    verify(child).startChild("op")
  }

  @Test
  fun `finish before materialize is buffered and replayed on the real child`() {
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    span.finish(SpanStatus.OK)
    val bufferedDate = span.finishDate

    val child = mock<ISpan>()
    span.materialize(child)

    verify(child).finish(SpanStatus.OK, bufferedDate)
  }

  @Test
  fun `finish before materialize is idempotent - first call wins`() {
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    span.finish(SpanStatus.OK)
    span.finish(SpanStatus.CANCELLED)

    val child = mock<ISpan>()
    span.materialize(child)

    verify(child).finish(eq(SpanStatus.OK), any())
    verify(child, never()).finish(eq(SpanStatus.CANCELLED), any())
  }

  @Test
  fun `materialize without a prior finish does not finish the child`() {
    val child = mock<ISpan>()
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    span.materialize(child)

    verify(child, never()).finish()
    verify(child, never()).finish(any())
    verify(child, never()).finish(any(), any())
  }

  @Test
  fun `finish after materialize delegates to the child`() {
    val child = mock<ISpan>()
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    span.materialize(child)

    span.finish(SpanStatus.OK)
    verify(child).finish(SpanStatus.OK)
  }

  @Test
  fun `before materialize, isFinished and finishDate reflect the buffered finish`() {
    val span = ExtendedAppStartSpan(SentryNanotimeDate())
    assertFalse(span.isFinished)

    span.finish(SpanStatus.OK)

    assertTrue(span.isFinished)
    assertNotNull(span.finishDate)
  }
}
