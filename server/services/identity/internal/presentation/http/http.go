package http

import (
	"identity/internal/domain/model"
	"identity/internal/domain/service"
	"net/http"
	"strings"

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
	{
		users.GET("/", h.Users)
		users.GET("/me", h.Me)
	}
}

func (h *UserHTTP) Users(ctx *gin.Context) {
	registeredUser, err := h.service.GetUsers()
	if err != nil {
		ctx.JSON(http.StatusConflict, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, registeredUser)
}

func (h *UserHTTP) Register(ctx *gin.Context) {
	var user model.User

	if err := ctx.ShouldBindJSON(&user); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	registeredUser, err := h.service.RegisterUser(&user)
	if err != nil {
		ctx.JSON(http.StatusConflict, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, registeredUser)
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
	authHeader := ctx.GetHeader("Authorization")
	if authHeader == "" {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Токен отсутствует"})
		return
	}

	tokenParts := strings.Split(authHeader, " ")
	if len(tokenParts) != 2 || tokenParts[0] != "Bearer" {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "Неверный формат токена"})
		return
	}

	token := tokenParts[1]

	user, err := h.service.Me(token)
	if err != nil {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, user)
}
