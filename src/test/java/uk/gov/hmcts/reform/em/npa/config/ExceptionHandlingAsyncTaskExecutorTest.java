package uk.gov.hmcts.reform.em.npa.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ExceptionHandlingAsyncTaskExecutorTest {

    @Mock
    private AsyncTaskExecutor delegateExecutor;

    private ExceptionHandlingAsyncTaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        taskExecutor = new ExceptionHandlingAsyncTaskExecutor(delegateExecutor);
    }

    @Test
    @DisplayName("execute(Runnable) successfully runs the task without exception")
    void executeShouldRunTaskSuccessfully() {
        Runnable mockTask = mock(Runnable.class);

        doAnswer(invocation -> {
            Runnable wrapped = invocation.getArgument(0);
            wrapped.run();
            return null;
        }).when(delegateExecutor).execute(any(Runnable.class));

        taskExecutor.execute(mockTask);

        verify(mockTask).run();
        verify(delegateExecutor).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("execute(Runnable) catches and handles exception without rethrowing")
    void executeShouldCatchAndHandleException() {
        Runnable failingTask = () -> {
            throw new RuntimeException("Task failure");
        };

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

        taskExecutor.execute(failingTask);

        verify(delegateExecutor).execute(captor.capture());
        Runnable wrappedRunnable = captor.getValue();

        assertThatCode(wrappedRunnable::run).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("submit(Runnable) delegates to executor and handles exceptions silently")
    @SuppressWarnings("unchecked")
    void submitRunnableShouldDelegateAndHandleException() {
        Future<Object> mockFuture = mock(Future.class);
        when(delegateExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable wrapped = invocation.getArgument(0);
            wrapped.run();
            return mockFuture;
        });

        Runnable failingTask = () -> {
            throw new IllegalStateException("Async runnable failed");
        };

        Future<?> result = taskExecutor.submit(failingTask);

        assertThat(result).isSameAs(mockFuture);
        verify(delegateExecutor).submit(any(Runnable.class));
    }

    @Test
    @DisplayName("submit(Callable) returns expected value on success")
    @SuppressWarnings("unchecked")
    void submitCallableShouldReturnResultOnSuccess() {
        Future<String> mockFuture = mock(Future.class);
        when(delegateExecutor.submit(any(Callable.class))).thenReturn(mockFuture);

        Callable<String> task = () -> "success";

        Future<String> result = taskExecutor.submit(task);

        assertThat(result).isSameAs(mockFuture);
        verify(delegateExecutor).submit(any(Callable.class));
    }

    @Test
    @DisplayName("submit(Callable) handles exception and rethrows it")
    @SuppressWarnings("unchecked")
    void submitCallableShouldHandleAndRethrowException() {
        ArgumentCaptor<Callable<String>> captor = ArgumentCaptor.forClass(Callable.class);

        Callable<String> failingTask = () -> {
            throw new RuntimeException("Callable failed");
        };

        taskExecutor.submit(failingTask);

        verify(delegateExecutor).submit(captor.capture());
        Callable<String> wrappedCallable = captor.getValue();

        assertThatThrownBy(wrappedCallable::call)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Callable failed");
    }

    @Test
    @DisplayName("handle() logs database exception when root cause is SQLException")
    void handleShouldProcessSQLExceptionRootCause() {
        SQLException sqlException = new SQLException("Unique constraint violation", "23505", 1062);
        RuntimeException wrappedException = new RuntimeException("Outer wrapper", sqlException);

        assertThatCode(() -> taskExecutor.handle(wrappedException))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handle() logs database exception with 'N/A' when DataAccessException has non-SQLException cause")
    void handleShouldProcessDataAccessExceptionWithoutSQLException() {
        DataAccessException dataAccessException = new DataAccessException("Non-SQL DB error") {};

        assertThatCode(() -> taskExecutor.handle(dataAccessException))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handle() logs generic exception when neither is a database error")
    void handleShouldProcessStandardException() {
        NullPointerException standardException = new NullPointerException("Null entity found");

        assertThatCode(() -> taskExecutor.handle(standardException))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("afterPropertiesSet() delegates when executor implements InitializingBean")
    void afterPropertiesSetShouldDelegateWhenApplicable() throws Exception {
        AsyncTaskExecutor customExecutor = mock(
            AsyncTaskExecutor.class,
            withSettings().extraInterfaces(InitializingBean.class)
        );
        ExceptionHandlingAsyncTaskExecutor executorWithInit =
            new ExceptionHandlingAsyncTaskExecutor(customExecutor);

        executorWithInit.afterPropertiesSet();

        verify((InitializingBean) customExecutor).afterPropertiesSet();
    }

    @Test
    @DisplayName("afterPropertiesSet() does nothing when executor does not implement InitializingBean")
    void afterPropertiesSetShouldNoOpWhenNotApplicable() {
        assertThatCode(() -> taskExecutor.afterPropertiesSet())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("destroy() delegates when executor implements DisposableBean")
    void destroyShouldDelegateWhenApplicable() throws Exception {
        AsyncTaskExecutor customExecutor = mock(
            AsyncTaskExecutor.class,
            withSettings().extraInterfaces(DisposableBean.class)
        );
        ExceptionHandlingAsyncTaskExecutor executorWithDestroy =
            new ExceptionHandlingAsyncTaskExecutor(customExecutor);

        executorWithDestroy.destroy();

        verify((DisposableBean) customExecutor).destroy();
    }

    @Test
    @DisplayName("destroy() does nothing when executor does not implement DisposableBean")
    void destroyShouldNoOpWhenNotApplicable() {
        assertThatCode(() -> taskExecutor.destroy())
            .doesNotThrowAnyException();
    }
}