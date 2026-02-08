-- =============================================
-- Flyway Migration V4
-- Atlas Platform - Posts, Comentarios y Encuestas
-- =============================================

-- =========================
-- POSTS
-- =========================
CREATE TABLE IF NOT EXISTS posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    author_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type ENUM('ANNOUNCEMENT','NEWS','AD') NOT NULL DEFAULT 'ANNOUNCEMENT',
    allow_comments BOOLEAN NOT NULL DEFAULT TRUE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_posts_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_posts_organization_id ON posts(organization_id);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_type ON posts(type);
CREATE INDEX idx_posts_is_pinned ON posts(is_pinned);

-- =========================
-- COMMENTS
-- =========================
CREATE TABLE IF NOT EXISTS comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    author_id INT NOT NULL,
    parent_id INT NULL COMMENT 'Para respuestas anidadas',
    content TEXT NOT NULL,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);

-- =========================
-- POLLS (Encuestas)
-- =========================
CREATE TABLE IF NOT EXISTS polls (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organization_id INT NOT NULL,
    author_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    allow_multiple BOOLEAN NOT NULL DEFAULT FALSE,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('DRAFT','ACTIVE','CLOSED') NOT NULL DEFAULT 'DRAFT',
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_polls_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE,
    CONSTRAINT fk_polls_author FOREIGN KEY (author_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_polls_organization_id ON polls(organization_id);
CREATE INDEX idx_polls_author_id ON polls(author_id);
CREATE INDEX idx_polls_status ON polls(status);

-- =========================
-- POLL_OPTIONS
-- =========================
CREATE TABLE IF NOT EXISTS poll_options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    poll_id INT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_poll_options_poll_id ON poll_options(poll_id);

-- =========================
-- POLL_VOTES
-- =========================
CREATE TABLE IF NOT EXISTS poll_votes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    poll_id INT NOT NULL,
    option_id INT NOT NULL,
    user_id INT NULL COMMENT 'NULL si is_anonymous',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_option FOREIGN KEY (option_id) REFERENCES poll_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_poll_votes_poll_id ON poll_votes(poll_id);
CREATE INDEX idx_poll_votes_option_id ON poll_votes(option_id);
CREATE INDEX idx_poll_votes_user_id ON poll_votes(user_id);

-- =========================
-- PERMISOS PARA POSTS Y ENCUESTAS
-- =========================
INSERT INTO permissions (code, name, description, module_code, resource, action) VALUES
('POSTS_CREATE', 'Crear Posts', 'Permite crear publicaciones', 'ATLAS_CORE', 'posts', 'CREATE'),
('POSTS_READ', 'Ver Posts', 'Permite ver publicaciones', 'ATLAS_CORE', 'posts', 'READ'),
('POSTS_UPDATE', 'Editar Posts', 'Permite editar publicaciones', 'ATLAS_CORE', 'posts', 'UPDATE'),
('POSTS_DELETE', 'Eliminar Posts', 'Permite eliminar publicaciones', 'ATLAS_CORE', 'posts', 'DELETE'),
('COMMENTS_CREATE', 'Crear Comentarios', 'Permite comentar publicaciones', 'ATLAS_CORE', 'comments', 'CREATE'),
('COMMENTS_DELETE', 'Eliminar Comentarios', 'Permite eliminar comentarios', 'ATLAS_CORE', 'comments', 'DELETE'),
('POLLS_CREATE', 'Crear Encuestas', 'Permite crear encuestas', 'ATLAS_CORE', 'polls', 'CREATE'),
('POLLS_READ', 'Ver Encuestas', 'Permite ver encuestas', 'ATLAS_CORE', 'polls', 'READ'),
('POLLS_VOTE', 'Votar en Encuestas', 'Permite votar en encuestas', 'ATLAS_CORE', 'polls', 'UPDATE'),
('POLLS_DELETE', 'Eliminar Encuestas', 'Permite eliminar encuestas', 'ATLAS_CORE', 'polls', 'DELETE');

-- =========================
-- ASIGNAR PERMISOS A ROLES
-- =========================

-- ADMIN_ATLAS: todos los permisos de posts y encuestas
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code = 'ADMIN_ATLAS' 
AND p.code IN ('POSTS_CREATE', 'POSTS_READ', 'POSTS_UPDATE', 'POSTS_DELETE', 
               'COMMENTS_CREATE', 'COMMENTS_DELETE',
               'POLLS_CREATE', 'POLLS_READ', 'POLLS_VOTE', 'POLLS_DELETE');

-- OWNER, TENANT, FAMILY: permisos de lectura y comentarios
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM role r, permissions p 
WHERE r.code IN ('OWNER', 'TENANT', 'FAMILY') 
AND p.code IN ('POSTS_READ', 'COMMENTS_CREATE', 'POLLS_READ', 'POLLS_VOTE');
