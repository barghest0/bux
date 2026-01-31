package service

import (
	"errors"
	"fmt"
	"strings"
	"user/internal/data/repository"
	"user/internal/domain/model"
	"user/internal/infra/auth"
)

var (
	ErrUserNotFound     = errors.New("user not found")
	ErrUsernameTaken    = errors.New("username already exists")
	ErrEmailTaken       = errors.New("email already exists")
	ErrInvalidEmail     = errors.New("invalid email")
	ErrInvalidPassword  = errors.New("invalid password")
	ErrPasswordTooShort = errors.New("password must be at least 8 characters")
	ErrPasswordMismatch = errors.New("current password is incorrect")
)

type UserService struct {
	repo *repository.UserRepository
}

func New(repo *repository.UserRepository) *UserService {
	return &UserService{
		repo: repo,
	}
}

func (s *UserService) RegisterUser(user *model.User) (*model.User, error) {
	const tag = "service.RegisterUser"

	if existingUser, _ := s.repo.GetUserByUsername(user.Username); existingUser != nil {
		return nil, fmt.Errorf("%s: %w", tag, ErrUsernameTaken)
	}

	if user.Email != "" {
		if !isValidEmail(user.Email) {
			return nil, fmt.Errorf("%s: %w", tag, ErrInvalidEmail)
		}
		if existingUser, _ := s.repo.GetUserByEmail(user.Email); existingUser != nil {
			return nil, fmt.Errorf("%s: %w", tag, ErrEmailTaken)
		}
	}

	hashedPassword, err := auth.HashPassword(user.Password)
	if err != nil {
		return nil, fmt.Errorf("%s: %w", tag, err)
	}
	user.Password = hashedPassword

	return s.repo.Create(user)
}

func (s *UserService) GetUsers() (*[]model.User, error) {
	const tag = "service.GetUsers"

	users, err := s.repo.GetAll()

	if err != nil {
		return nil, fmt.Errorf("%s: %w", tag, err)
	}

	return users, nil
}

func (s *UserService) Login(username string, password string) (*model.User, string, error) {
	const tag = "service.Login"

	user, err := s.repo.GetUserByUsername(username)
	if err != nil || user == nil {
		return nil, "", fmt.Errorf("%s: %s", tag, "Неверные данные")
	}

	if !auth.CheckPasswordHash(password, user.Password) {
		return nil, "", fmt.Errorf("%s: %s", tag, "Неверные данные")
	}

	token, err := auth.GenerateToken(user.ID)
	if err != nil {
		return nil, "", fmt.Errorf("%s: %s", tag, "Ошибка генерации токена")
	}

	return user, token, nil
}

func (s *UserService) Me(userID int) (*model.User, error) {
	const tag = "service.Me"

	user, err := s.repo.GetByID(userID)
	if err != nil || user == nil {
		return nil, fmt.Errorf("%s: %w", tag, ErrUserNotFound)
	}

	return user, nil
}

func (s *UserService) UpdateProfile(userID int, username, email string) (*model.User, error) {
	const tag = "service.UpdateProfile"

	user, err := s.repo.GetByID(userID)
	if err != nil || user == nil {
		return nil, fmt.Errorf("%s: %w", tag, ErrUserNotFound)
	}

	if username != "" && username != user.Username {
		if existing, _ := s.repo.GetUserByUsername(username); existing != nil {
			return nil, fmt.Errorf("%s: %w", tag, ErrUsernameTaken)
		}
		user.Username = username
	}

	if email != "" && email != user.Email {
		if !isValidEmail(email) {
			return nil, fmt.Errorf("%s: %w", tag, ErrInvalidEmail)
		}
		if existing, _ := s.repo.GetUserByEmail(email); existing != nil {
			return nil, fmt.Errorf("%s: %w", tag, ErrEmailTaken)
		}
		user.Email = email
	}

	return s.repo.Update(user)
}

func (s *UserService) UpdatePassword(userID int, currentPassword, newPassword string) error {
	const tag = "service.UpdatePassword"

	user, err := s.repo.GetByID(userID)
	if err != nil || user == nil {
		return fmt.Errorf("%s: %w", tag, ErrUserNotFound)
	}

	if !auth.CheckPasswordHash(currentPassword, user.Password) {
		return fmt.Errorf("%s: %w", tag, ErrPasswordMismatch)
	}

	if len(newPassword) < 8 {
		return fmt.Errorf("%s: %w", tag, ErrPasswordTooShort)
	}

	hashedPassword, err := auth.HashPassword(newPassword)
	if err != nil {
		return fmt.Errorf("%s: %w", tag, ErrInvalidPassword)
	}

	user.Password = hashedPassword
	if _, err := s.repo.Update(user); err != nil {
		return fmt.Errorf("%s: %w", tag, err)
	}

	return nil
}

func isValidEmail(email string) bool {
	at := strings.Index(email, "@")
	if at <= 0 || at == len(email)-1 {
		return false
	}
	return strings.Contains(email[at+1:], ".")
}
