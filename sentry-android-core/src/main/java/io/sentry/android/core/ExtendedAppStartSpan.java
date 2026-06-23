package io.sentry.android.core;

import io.sentry.BaggageHeader;
import io.sentry.ISentryLifecycleToken;
import io.sentry.ISpan;
import io.sentry.Instrumenter;
import io.sentry.MeasurementUnit;
import io.sentry.NoOpSpan;
import io.sentry.SentryDate;
import io.sentry.SentryTraceHeader;
import io.sentry.SpanContext;
import io.sentry.SpanOptions;
import io.sentry.SpanStatus;
import io.sentry.TraceContext;
import io.sentry.TracesSamplingDecision;
import io.sentry.protocol.Contexts;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A deferred {@link ISpan} returned by {@code Sentry.getExtendedAppStartSpan()}. It is created in
 * {@code Application.onCreate} - before the app start transaction exists - and later {@link
 * #materialize(ISpan) materialized} into a real child span once the transaction is created.
 *
 * <p>Before materialization, all calls delegate to a {@link NoOpSpan}, except {@code finish(...)},
 * which buffers the status and timestamp (defaulting the timestamp to the call time) so an early
 * finish keeps its real end time. Once materialized, a buffered finish is replayed on the real
 * child and all calls delegate to it.
 */
@ApiStatus.Internal
public final class ExtendedAppStartSpan implements ISpan {

  private final @NotNull SentryDate startDate;
  private volatile @Nullable ISpan delegate;

  private @Nullable SpanStatus bufferedStatus;
  private @Nullable SentryDate bufferedFinishDate;
  private volatile boolean finished;

  public ExtendedAppStartSpan(final @NotNull SentryDate startDate) {
    this.startDate = startDate;
  }

  /**
   * Swaps the no-op delegate for the real child span. If this span was already finished before
   * materialization, the buffered finish is immediately replayed on the real child.
   */
  public void materialize(final @NotNull ISpan realChild) {
    this.delegate = realChild;
    if (finished) {
      realChild.finish(bufferedStatus, bufferedFinishDate);
    }
  }

  private @NotNull ISpan delegate() {
    final @Nullable ISpan d = delegate;
    return d != null ? d : NoOpSpan.getInstance();
  }

  private void bufferFinish(
      final @Nullable SpanStatus status, final @Nullable SentryDate timestamp) {
    if (finished) {
      return;
    }
    finished = true;
    bufferedStatus = status;
    bufferedFinishDate =
        timestamp != null ? timestamp : AndroidDateUtils.getCurrentSentryDateTime();
  }

  @Override
  public void finish() {
    final @Nullable ISpan d = delegate;
    if (d != null) {
      d.finish();
      return;
    }
    bufferFinish(null, null);
  }

  @Override
  public void finish(final @Nullable SpanStatus status) {
    final @Nullable ISpan d = delegate;
    if (d != null) {
      d.finish(status);
      return;
    }
    bufferFinish(status, null);
  }

  @Override
  public void finish(final @Nullable SpanStatus status, final @Nullable SentryDate timestamp) {
    final @Nullable ISpan d = delegate;
    if (d != null) {
      d.finish(status, timestamp);
      return;
    }
    bufferFinish(status, timestamp);
  }

  @Override
  public @NotNull SentryDate getStartDate() {
    return startDate;
  }

  @Override
  public @Nullable SentryDate getFinishDate() {
    final @Nullable ISpan d = delegate;
    return d != null ? d.getFinishDate() : bufferedFinishDate;
  }

  @Override
  public boolean isFinished() {
    final @Nullable ISpan d = delegate;
    return d != null ? d.isFinished() : finished;
  }

  @Override
  public @NotNull ISpan startChild(final @NotNull String operation) {
    return delegate().startChild(operation);
  }

  @Override
  public @NotNull ISpan startChild(
      final @NotNull String operation,
      final @Nullable String description,
      final @NotNull SpanOptions spanOptions) {
    return delegate().startChild(operation, description, spanOptions);
  }

  @Override
  public @NotNull ISpan startChild(
      final @NotNull SpanContext spanContext, final @NotNull SpanOptions spanOptions) {
    return delegate().startChild(spanContext, spanOptions);
  }

  @Override
  public @NotNull ISpan startChild(
      final @NotNull String operation,
      final @Nullable String description,
      final @Nullable SentryDate timestamp,
      final @NotNull Instrumenter instrumenter) {
    return delegate().startChild(operation, description, timestamp, instrumenter);
  }

  @Override
  public @NotNull ISpan startChild(
      final @NotNull String operation,
      final @Nullable String description,
      final @Nullable SentryDate timestamp,
      final @NotNull Instrumenter instrumenter,
      final @NotNull SpanOptions spanOptions) {
    return delegate().startChild(operation, description, timestamp, instrumenter, spanOptions);
  }

  @Override
  public @NotNull ISpan startChild(
      final @NotNull String operation, final @Nullable String description) {
    return delegate().startChild(operation, description);
  }

  @Override
  public @NotNull SentryTraceHeader toSentryTrace() {
    return delegate().toSentryTrace();
  }

  @Override
  public @Nullable TraceContext traceContext() {
    return delegate().traceContext();
  }

  @Override
  public @Nullable BaggageHeader toBaggageHeader(
      final @Nullable List<String> thirdPartyBaggageHeaders) {
    return delegate().toBaggageHeader(thirdPartyBaggageHeaders);
  }

  @Override
  public void setOperation(final @NotNull String operation) {
    delegate().setOperation(operation);
  }

  @Override
  public @NotNull String getOperation() {
    return delegate().getOperation();
  }

  @Override
  public void setDescription(final @Nullable String description) {
    delegate().setDescription(description);
  }

  @Override
  public @Nullable String getDescription() {
    return delegate().getDescription();
  }

  @Override
  public void setStatus(final @Nullable SpanStatus status) {
    delegate().setStatus(status);
  }

  @Override
  public @Nullable SpanStatus getStatus() {
    return delegate().getStatus();
  }

  @Override
  public void setThrowable(final @Nullable Throwable throwable) {
    delegate().setThrowable(throwable);
  }

  @Override
  public @Nullable Throwable getThrowable() {
    return delegate().getThrowable();
  }

  @Override
  public @NotNull SpanContext getSpanContext() {
    return delegate().getSpanContext();
  }

  @Override
  public void setTag(final @Nullable String key, final @Nullable String value) {
    delegate().setTag(key, value);
  }

  @Override
  public @Nullable String getTag(final @Nullable String key) {
    return delegate().getTag(key);
  }

  @Override
  public void setData(final @Nullable String key, final @Nullable Object value) {
    delegate().setData(key, value);
  }

  @Override
  public @Nullable Object getData(final @Nullable String key) {
    return delegate().getData(key);
  }

  @Override
  public void setMeasurement(final @NotNull String name, final @NotNull Number value) {
    delegate().setMeasurement(name, value);
  }

  @Override
  public void setMeasurement(
      final @NotNull String name,
      final @NotNull Number value,
      final @NotNull MeasurementUnit unit) {
    delegate().setMeasurement(name, value, unit);
  }

  @Override
  public boolean updateEndDate(final @NotNull SentryDate date) {
    return delegate().updateEndDate(date);
  }

  @Override
  public boolean isNoOp() {
    return delegate().isNoOp();
  }

  @Override
  public void setContext(final @Nullable String key, final @Nullable Object context) {
    delegate().setContext(key, context);
  }

  @Override
  public @NotNull Contexts getContexts() {
    return delegate().getContexts();
  }

  @Override
  public @Nullable Boolean isSampled() {
    return delegate().isSampled();
  }

  @Override
  public @Nullable TracesSamplingDecision getSamplingDecision() {
    return delegate().getSamplingDecision();
  }

  @Override
  public @NotNull ISentryLifecycleToken makeCurrent() {
    return delegate().makeCurrent();
  }

  @Override
  public void addFeatureFlag(final @Nullable String flag, final @Nullable Boolean result) {
    delegate().addFeatureFlag(flag, result);
  }
}
