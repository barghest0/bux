package auth

import (
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

var JwtKey = []byte("key")

func ParseToken(tokenString string) (uint, error) {

	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (
		interface{},
		error,
	) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return JwtKey, nil
	})

	if err != nil {
		return 0, err
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		userID := uint(claims["sub"].(float64))
		expiration := claims["exp"].(float64)

		expirationTime := time.Unix(int64(expiration), 0)
		if time.Now().After(expirationTime) {
			return 0, errors.New("token has expired")
		}

		return userID, nil
	}

	return 0, errors.New("invalid token")
}
