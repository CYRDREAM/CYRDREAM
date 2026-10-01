CREATE TABLE IF NOT EXISTS rt_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  CONSTRAINT uk_rt_username UNIQUE (username)
);
CREATE TABLE IF NOT EXISTS rt_books (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  author VARCHAR(100),
  total_pages INT NOT NULL,
  read_pages INT NOT NULL DEFAULT 0,
  status VARCHAR(10) NOT NULL DEFAULT 'UNREAD',
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_rt_books_user FOREIGN KEY (user_id) REFERENCES rt_users(id),
  CONSTRAINT ck_rt_pages CHECK (total_pages > 0 AND read_pages >= 0 AND read_pages <= total_pages),
  CONSTRAINT ck_rt_status CHECK (status IN ('UNREAD','READING','READ'))
);
