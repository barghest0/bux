package auth

import (
	"errors"
	"log"
	"os"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

var (
	jwtKey     []byte
	jwtKeyOnce sync.Once
)

func getJWTKey() []byte {
	jwtKeyOnce.Do(func() {
		secret := os.Getenv("JWT_SECRET")
		if secret == "" {
			log.Fatal("JWT_SECRET environment variable is required")
		}
		if len(secret) < 32 {
			log.Fatal("JWT_SECRET must be at least 32 characters")
		}
		jwtKey = []byte(secret)
	})
	return jwtKey
}

func GenerateToken(sub int) (string, error) {
	claims := jwt.MapClaims{
		"sub": sub,
		"exp": time.Now().Add(time.Hour * 24).Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString(getJWTKey())
	if err != nil {
		return "", err
	}

	return tokenString, nil
}

func ParseToken(tokenString string) (int, error) {

	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (
		interface{},
		error,
	) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return getJWTKey(), nil
	})

	if err != nil {
		return 0, err
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		userID := int(claims["sub"].(float64))
		expiration := claims["exp"].(float64)

		// Проверка срока действия токена
		expirationTime := time.Unix(int64(expiration), 0)
		if time.Now().After(expirationTime) {
			return 0, errors.New("token has expired")
		}

		return userID, nil
	}

	return 0, errors.New("invalid token")
}
