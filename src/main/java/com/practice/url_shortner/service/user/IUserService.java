package com.practice.url_shortner.service.user;

import com.practice.url_shortner.model.AuthenticationRequestRecord;
import com.practice.url_shortner.model.AuthenticationResponseRecord;
import com.practice.url_shortner.model.ChangePasswordRequestRecord;
import com.practice.url_shortner.model.CreateUserRequest;
import com.practice.url_shortner.model.PasswordResetRequestRecord;
import com.practice.url_shortner.model.RecoveryCodeResponseRecord;

public interface IUserService {

	AuthenticationResponseRecord login(AuthenticationRequestRecord credentials);

	/** Trades a refresh token for a new access token and a new refresh token. The old refresh token is spent. */
	AuthenticationResponseRecord refresh(String refreshToken);

	/** Revokes the given JWT and the session it belongs to. The JWT is rejected for the rest of its lifetime. */
	void logout(String token);

	/**
	 * Creates the account and its first recovery code.
	 *
	 * @return the recovery code, which is not stored and cannot be shown again
	 */
	RecoveryCodeResponseRecord register(CreateUserRequest request);

	/**
	 * Replaces the user's password after checking the current one, and ends all of
	 * the user's sessions.
	 */
	void changePassword(String userName, ChangePasswordRequestRecord request);

	/** Disables the account after checking the password, and ends all of the user's sessions. */
	void deactivate(String userName, String password);

	/**
	 * Replaces the user's recovery code after checking the password.
	 *
	 * @return the new code
	 */
	RecoveryCodeResponseRecord regenerateRecoveryCode(String userName, String password);

	/**
	 * Sets a new password for the user who presents their recovery code, ends all
	 * of the user's sessions, and replaces the used code.
	 *
	 * @return the replacement recovery code
	 * @throws com.practice.url_shortner.exception.CustomException with
	 *         {@code INVALID_CREDENTIALS} for an unknown user, a wrong code or a
	 *         disabled account, so the three cannot be told apart
	 */
	RecoveryCodeResponseRecord resetPassword(PasswordResetRequestRecord request);

	void getUser(String userName);

	void getUserById(int userId);
}
