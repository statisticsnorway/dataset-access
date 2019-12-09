package no.ssb.datasetaccess.health;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

public class HealthAwarePgPool implements PgPool {

    final PgPool delegate;
    final AtomicReference<ReadinessSample> lastReadySample;

    public HealthAwarePgPool(PgPool delegate, AtomicReference<ReadinessSample> lastReadySample) {
        this.delegate = delegate;
        this.lastReadySample = lastReadySample;
    }

    Handler<AsyncResult<RowSet<Row>>> handleRowSet(Handler<AsyncResult<RowSet<Row>>> handler) {
        return ar -> {
            lastReadySample.set(new ReadinessSample(ar.succeeded(), System.currentTimeMillis()));
            handler.handle(ar);
        };
    }

    <R> Handler<AsyncResult<SqlResult<R>>> handleSqlResult(Handler<AsyncResult<SqlResult<R>>> handler) {
        return ar -> {
            lastReadySample.set(new ReadinessSample(ar.succeeded(), System.currentTimeMillis()));
            handler.handle(ar);
        };
    }

    @Override
    public PgPool preparedQuery(String sql, Handler<AsyncResult<RowSet<Row>>> handler) {
        return delegate.preparedQuery(sql, handleRowSet(handler));
    }

    @Override
    public <R> PgPool preparedQuery(String sql, Collector<Row, ?, R> collector, Handler<AsyncResult<SqlResult<R>>> handler) {
        return delegate.preparedQuery(sql, collector, handleSqlResult(handler));
    }

    @Override
    public PgPool query(String sql, Handler<AsyncResult<RowSet<Row>>> handler) {
        return delegate.query(sql, handleRowSet(handler));
    }

    @Override
    public <R> PgPool query(String sql, Collector<Row, ?, R> collector, Handler<AsyncResult<SqlResult<R>>> handler) {
        return delegate.query(sql, collector, handleSqlResult(handler));
    }

    @Override
    public PgPool preparedQuery(String sql, Tuple arguments, Handler<AsyncResult<RowSet<Row>>> handler) {
        return delegate.preparedQuery(sql, arguments, handleRowSet(handler));
    }

    @Override
    public <R> PgPool preparedQuery(String sql, Tuple arguments, Collector<Row, ?, R> collector, Handler<AsyncResult<SqlResult<R>>> handler) {
        return delegate.preparedQuery(sql, arguments, collector, handleSqlResult(handler));
    }

    @Override
    public PgPool preparedBatch(String sql, List<Tuple> batch, Handler<AsyncResult<RowSet<Row>>> handler) {
        return delegate.preparedBatch(sql, batch, handleRowSet(handler));
    }

    @Override
    public <R> PgPool preparedBatch(String sql, List<Tuple> batch, Collector<Row, ?, R> collector, Handler<AsyncResult<SqlResult<R>>> handler) {
        return delegate.preparedBatch(sql, batch, collector, handleSqlResult(handler));
    }

    @Override
    public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
        delegate.getConnection(handler);
    }

    @Override
    public void begin(Handler<AsyncResult<Transaction>> handler) {
        delegate.begin(handler);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
