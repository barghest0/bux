package service

import (
	"fmt"
	"identity/internal/data/repository"
	"identity/internal/domain/model"
	"identity/internal/infra/auth"
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
		return nil, fmt.Errorf("%s: %s", tag, "Пользователь уже существует")
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
		return nil, fmt.Errorf("%s: %s", tag, "Пользователь не найден")
	}

	return user, nil
}
