package com.practice.url_shortner.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.repository.IUserRepository;

/**
 * Runs against the H2 test schema, which gets its unique index on
 * users.username from the entity's {@code @Column(unique = true)}.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserDetailsManagerImplConcurrencyTest {

	private static final String USERNAME = "race-user";

	@Autowired
	private UserDetailsManagerImpl userDetailsManager;

	@Autowired
	private IUserRepository userRepository;

	@AfterEach
	void cleanUp() {
		userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);
	}

	@Test
	void createUser_sequentialDuplicate_isRejectedByPreCheck() {
		userDetailsManager.createUser(newUser());

		CustomException ex = org.junit.jupiter.api.Assertions.assertThrows(CustomException.class,
				() -> userDetailsManager.createUser(newUser()));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NAME_NOT_AVAILABLE);
	}

	@Test
	void createUser_concurrentDuplicates_exactlyOneSucceedsAndOtherGetsUserNameNotAvailable() throws Exception {
		int threads = 2;
		CyclicBarrier barrier = new CyclicBarrier(threads);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			List<Future<Throwable>> futures = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				Callable<Throwable> task = () -> {
					barrier.await();
					try {
						userDetailsManager.createUser(newUser());
						return null;
					} catch (Throwable t) {
						return t;
					}
				};
				futures.add(executor.submit(task));
			}

			int successes = 0;
			List<Throwable> failures = new ArrayList<>();
			for (Future<Throwable> future : futures) {
				Throwable result = future.get();
				if (result == null) {
					successes++;
				} else {
					failures.add(result);
				}
			}

			assertThat(successes).isEqualTo(1);
			assertThat(failures).hasSize(1);
			assertThat(failures.get(0)).isInstanceOf(CustomException.class);
			assertThat(((CustomException) failures.get(0)).getErrorCode()).isEqualTo(ErrorCode.USER_NAME_NOT_AVAILABLE);
		} finally {
			executor.shutdownNow();
		}
	}

	private User newUser() {
		return new User(USERNAME, "encoded-password", java.util.Collections.emptyList());
	}
}
