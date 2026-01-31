package http

import (
	"errors"
	"net/http"
	"user/internal/domain/model"
	"user/internal/domain/service"
	"user/internal/infra/auth"
	"user/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type UserHTTP struct {
	service *service.UserService
}

func New(r *gin.Engine, s *service.UserService) {
	h := &UserHTTP{
		service: s,
	}

	auth := r.Group("/auth")
	{
		auth.POST("/register", h.Register)
		auth.POST("/login", h.Login)
	}

	users := r.Group("/users")
	users.Use(middleware.AuthMiddleware())
	{
		users.GET("/", h.Users)
		users.GET("/me", h.Me)
		users.PUT("/profile", h.UpdateProfile)
		users.PUT("/password", h.UpdatePassword)
	}
}

func (h *UserHTTP) Users(ctx *gin.Context) {
	registeredUser, err := h.service.GetUsers()
	if err != nil {
		ctx.JSON(http.StatusConflict, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, registeredUser)
}

func (h *UserHTTP) Register(ctx *gin.Context) {
	var user model.User

	if err := ctx.ShouldBindJSON(&user); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	registeredUser, err := h.service.RegisterUser(&user)
	if err != nil {
		status := http.StatusConflict
		if errors.Is(err, service.ErrInvalidEmail) {
			status = http.StatusBadRequest
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	token, err := auth.GenerateToken(registeredUser.ID)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, gin.H{
		"user":  registeredUser,
		"token": token,
	})
}

func (h *UserHTTP) Login(ctx *gin.Context) {
	var UserCredentials struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}

	if err := ctx.ShouldBindJSON(&UserCredentials); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	user, token, err := h.service.Login(UserCredentials.Username, UserCredentials.Password)
	if err != nil {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, gin.H{
		"user":  user,
		"token": token,
	})
}

func (h *UserHTTP) Me(ctx *gin.Context) {
	userID, exists := ctx.Get("userID")
	if !exists {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Пользователь не найден в контексте"})
		return
	}

	user, err := h.service.Me(userID.(int))
	if err != nil {
		status := http.StatusUnauthorized
		if errors.Is(err, service.ErrUserNotFound) {
			status = http.StatusNotFound
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, user)
}

func (h *UserHTTP) UpdateProfile(ctx *gin.Context) {
	userID, exists := ctx.Get("userID")
	if !exists {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Пользователь не найден в контексте"})
		return
	}

	var req struct {
		Username string `json:"username"`
		Email    string `json:"email"`
	}
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	user, err := h.service.UpdateProfile(userID.(int), req.Username, req.Email)
	if err != nil {
		status := http.StatusBadRequest
		switch {
		case errors.Is(err, service.ErrUserNotFound):
			status = http.StatusNotFound
		case errors.Is(err, service.ErrUsernameTaken), errors.Is(err, service.ErrEmailTaken):
			status = http.StatusConflict
		case errors.Is(err, service.ErrInvalidEmail):
			status = http.StatusBadRequest
		default:
			status = http.StatusInternalServerError
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, user)
}

func (h *UserHTTP) UpdatePassword(ctx *gin.Context) {
	userID, exists := ctx.Get("userID")
	if !exists {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Пользователь не найден в контексте"})
		return
	}

	var req struct {
		CurrentPassword string `json:"current_password" binding:"required"`
		NewPassword     string `json:"new_password" binding:"required"`
	}
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if err := h.service.UpdatePassword(userID.(int), req.CurrentPassword, req.NewPassword); err != nil {
		status := http.StatusBadRequest
		switch {
		case errors.Is(err, service.ErrUserNotFound):
			status = http.StatusNotFound
		case errors.Is(err, service.ErrPasswordMismatch):
			status = http.StatusUnauthorized
		case errors.Is(err, service.ErrPasswordTooShort):
			status = http.StatusBadRequest
		default:
			status = http.StatusInternalServerError
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, gin.H{"status": "ok"})
}
