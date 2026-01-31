package model

type User struct {
	ID       int    `json:"id" gorm:"primaryKey"`
	Username string `json:"username" gorm:"not null"`
	Email    string `json:"email" gorm:"uniqueIndex"`
	Password string `json:"-" gorm:"not null"`
}
