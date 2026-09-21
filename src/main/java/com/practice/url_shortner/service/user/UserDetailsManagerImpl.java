package com.practice.url_shortner.service.user;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.practice.url_shortner.constants.ErrorCode;
import com.practice.url_shortner.entity.UserEntity;
import com.practice.url_shortner.exception.CustomException;
import com.practice.url_shortner.mapper.IUserDetailsToUserEntityMapper;
import com.practice.url_shortner.mapper.IUserEntityToUserDetailsMapper;
import com.practice.url_shortner.repository.IUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsManagerImpl implements UserDetailsManager, UserDetailsPasswordService {

	private final IUserRepository userRepository;

	private final IUserEntityToUserDetailsMapper userToUserDetailsMapper;

	private final IUserDetailsToUserEntityMapper userDetailsToUserMapper;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (StringUtils.isBlank(username)) {
			log.info("Invalid user name {}", username);
			throw new UsernameNotFoundException("Invalid user name.");
		}

		log.debug("Loading user {} from database.", username);
		UserEntity userEntity = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found."));
		log.debug("User {} found.", username);
		UserDetails userDetails = userToUserDetailsMapper.convert(userEntity);
		return userDetails;
	}

	@Override
	public UserDetails updatePassword(UserDetails user, @Nullable String newPassword) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
	public void createUser(UserDetails user) {
		log.debug("Request received to create new user: {}", user.getUsername());
		if (StringUtils.isAnyBlank(user.getUsername(), user.getPassword())) {
			log.info("Invalid username or password. User {} not created.", user.getUsername());
			throw new IllegalArgumentException("Invalid user credentials provided.");
		}

		log.debug("Checking if user {} already exists.", user.getUsername());
		if (userExists(user.getUsername())) {
			log.info("User with name {} already exists.", user.getUsername());
			throw CustomException.builder().errorCode(ErrorCode.USER_NAME_NOT_AVAILABLE).build();
		}

		UserEntity userEntity = userDetailsToUserMapper.convert((User) user);
		//		userEntity.setPassword(user.getPassword());
		userEntity.setCreatedAt(LocalDateTime.now());
		userEntity.setUpdatedAt(LocalDateTime.now());
		userEntity.setAccountNonExpired(true);
		userEntity.setAccountNonLocked(true);
		userEntity.setCredentialsNonExpired(true);
		userEntity.setEnabled(true);

		UserEntity savedEntity = userRepository.save(userEntity);
		log.debug("User {} created with id {}.", user.getUsername(), savedEntity.getId());
	}

	@Override
	public void updateUser(UserDetails user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteUser(String username) {
		// TODO Auto-generated method stub

	}

	@Override
	public void changePassword(@Nullable String oldPassword, @Nullable String newPassword) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean userExists(String username) {
		if(StringUtils.isBlank(username)) {
			log.info("Invalid user name {}", username);
			return false;
		}
		log.debug("Checking if {} exists.", username);
		boolean isUserExists = userRepository.userExists(username);
		log.debug("User exists? {}", isUserExists);
		return isUserExists;
	}

}
