-- AvtoTest Platform - Initial Database Schema
-- File: V001__init.sql
-- Description: Traffic rules learning and exam platform for Uzbekistan
-- Platforms: Web Admin Panel, Android App, iOS App

-- ==========================================
-- ENUMS AND TYPES
-- ==========================================

CREATE TYPE user_status AS ENUM ('active', 'inactive', 'blocked', 'pending_verification');
CREATE TYPE exam_type AS ENUM ('practice', 'mock', 'final', 'daily_challenge');
CREATE TYPE difficulty_level AS ENUM ('easy', 'medium', 'hard');
CREATE TYPE exam_status AS ENUM ('in_progress', 'completed', 'abandoned', 'expired');
CREATE TYPE mastery_level AS ENUM ('beginner', 'intermediate', 'advanced', 'expert');
CREATE TYPE platform_type AS ENUM ('android', 'ios', 'web');
CREATE TYPE notification_priority AS ENUM ('low', 'normal', 'high', 'urgent');
CREATE TYPE achievement_rarity AS ENUM ('common', 'rare', 'epic', 'legendary');

-- ==========================================
-- CORE TABLES - RBAC (Role-Based Access Control)
-- ==========================================

-- Privileges table
CREATE TABLE privileges (
    name VARCHAR(100) PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Role-Privilege mapping
CREATE TABLE role_privileges (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    privilege VARCHAR(100) NOT NULL REFERENCES privileges(name) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, privilege)
);

-- ==========================================
-- USERS TABLE (Admin, Teacher, Student in one table)
-- ==========================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Authentication
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- SCrypt hashed

    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,

    -- Role & Status
    role_id UUID NOT NULL REFERENCES roles(id),
    status user_status DEFAULT 'pending_verification',

    -- Verification
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_phone_check CHECK (phone ~* '^\+998[0-9]{9}$')
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_phone ON users(phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_last_login ON users(last_login_at DESC);

-- ==========================================
-- REFRESH TOKENS (Stateless JWT)
-- ==========================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,  -- SHA-256 hash

    -- Device Information
    device_info JSONB,  -- {platform: "android", device_id: "...", model: "...", os_version: "...", app_version: "1.0.0"}
    ip_address INET,
    user_agent TEXT,

    -- Lifecycle
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoke_reason VARCHAR(255),  -- "logout", "security", "token_rotation"

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for refresh tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash) WHERE NOT revoked;
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE NOT revoked;

-- ==========================================
-- QUESTION BANK STRUCTURE
-- ==========================================

-- Categories (Topics/Mavzular)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,

    -- Multilingual names
    name_uz VARCHAR(255) NOT NULL,
    name_uzk VARCHAR(255),
    name_ru VARCHAR(255),
    name_en VARCHAR(255),

    -- Multilingual descriptions
    description_uz TEXT,
    description_uzk TEXT,
    description_ru TEXT,
    description_en TEXT,

    -- Hierarchy
    parent_id INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    order_index INTEGER DEFAULT 0,

    -- Visual
    icon_url VARCHAR(500),
    color VARCHAR(7),  -- HEX color for UI

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_active ON categories(is_active);
CREATE INDEX idx_categories_order ON categories(order_index);

-- Questions
CREATE TABLE questions (
    id SERIAL PRIMARY KEY,
    category_id INTEGER REFERENCES categories(id) ON DELETE SET NULL,

    -- Question text (multilingual)
    question_uz TEXT NOT NULL,
    question_uzk TEXT,
    question_ru TEXT,
    question_en TEXT,

    -- Question image
    image_key VARCHAR(255),  -- e.g., "i1_1" from JSON
    image_url VARCHAR(500),  -- Full CDN/S3 URL

    -- Explanation (multilingual)
    explanation_uz TEXT,
    explanation_uzk TEXT,
    explanation_ru TEXT,
    explanation_en TEXT,

    -- Metadata
    difficulty difficulty_level DEFAULT 'medium',
    is_active BOOLEAN DEFAULT TRUE,

    -- Analytics
    usage_count INTEGER DEFAULT 0,
    correct_rate DECIMAL(5,2),  -- Percentage of correct answers
    average_time_seconds INTEGER,  -- Average time spent on this question

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_category ON questions(category_id);
CREATE INDEX idx_questions_active ON questions(is_active);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_questions_usage ON questions(usage_count DESC);

-- Question Options (Answer choices)
CREATE TABLE question_options (
    id SERIAL PRIMARY KEY,
    question_id INTEGER NOT NULL REFERENCES questions(id) ON DELETE CASCADE,

    -- Option text (multilingual)
    option_text_uz TEXT NOT NULL,
    option_text_uzk TEXT,
    option_text_ru TEXT,
    option_text_en TEXT,

    -- Option image (if any)
    option_image_url VARCHAR(500),

    -- Correctness
    is_correct BOOLEAN DEFAULT FALSE,
    order_index INTEGER NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT question_options_order_check CHECK (order_index BETWEEN 1 AND 5)
);

CREATE INDEX idx_question_options_question ON question_options(question_id);
CREATE INDEX idx_question_options_correct ON question_options(question_id, is_correct);
CREATE UNIQUE INDEX idx_question_options_order ON question_options(question_id, order_index);

-- ==========================================
-- EXAM SYSTEM
-- ==========================================

-- Exam Templates (Created by admins)
CREATE TABLE exam_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Title (multilingual)
    title_uz VARCHAR(255) NOT NULL,
    title_uzk VARCHAR(255),
    title_ru VARCHAR(255),
    title_en VARCHAR(255),

    -- Description (multilingual)
    description_uz TEXT,
    description_uzk TEXT,
    description_ru TEXT,
    description_en TEXT,

    -- Exam configuration
    exam_type exam_type NOT NULL,
    total_questions INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    passing_score INTEGER NOT NULL,  -- Out of 100

    -- Question selection strategy
    question_selection JSONB,  -- {strategy: "random", category_weights: {...}, difficulty_mix: {...}}

    -- Metadata
    created_by UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exam_templates_type ON exam_templates(exam_type);
CREATE INDEX idx_exam_templates_active ON exam_templates(is_active);
CREATE INDEX idx_exam_templates_created_by ON exam_templates(created_by);

-- Student Exam Attempts
CREATE TABLE student_exam_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_template_id UUID NOT NULL REFERENCES exam_templates(id),

    -- Timing
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,

    -- Results
    total_questions INTEGER NOT NULL,
    correct_answers INTEGER DEFAULT 0,
    wrong_answers INTEGER DEFAULT 0,
    unanswered INTEGER DEFAULT 0,
    score DECIMAL(5,2),  -- Out of 100
    passed BOOLEAN,

    -- Status
    status exam_status DEFAULT 'in_progress',

    -- Device info
    device_info JSONB,  -- {platform: "android", app_version: "1.0.0", device_model: "..."}

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exam_attempts_student ON student_exam_attempts(student_id);
CREATE INDEX idx_exam_attempts_template ON student_exam_attempts(exam_template_id);
CREATE INDEX idx_exam_attempts_status ON student_exam_attempts(status);
CREATE INDEX idx_exam_attempts_started ON student_exam_attempts(started_at DESC);
CREATE INDEX idx_exam_attempts_completed ON student_exam_attempts(completed_at DESC) WHERE completed_at IS NOT NULL;

-- Student Answers
CREATE TABLE student_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES student_exam_attempts(id) ON DELETE CASCADE,
    question_id INTEGER NOT NULL REFERENCES questions(id),
    selected_option_id INTEGER REFERENCES question_options(id),

    -- Answer metadata
    is_correct BOOLEAN,
    time_spent_seconds INTEGER,
    answered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Flagged for review
    is_flagged BOOLEAN DEFAULT FALSE,

    CONSTRAINT unique_answer_per_attempt UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_student_answers_attempt ON student_answers(attempt_id);
CREATE INDEX idx_student_answers_question ON student_answers(question_id);
CREATE INDEX idx_student_answers_correct ON student_answers(is_correct);

-- ==========================================
-- GAMIFICATION
-- ==========================================

-- Achievements
CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Name (multilingual)
    name_uz VARCHAR(255) NOT NULL,
    name_uzk VARCHAR(255),
    name_ru VARCHAR(255),
    name_en VARCHAR(255),

    -- Description (multilingual)
    description_uz TEXT,
    description_uzk TEXT,
    description_ru TEXT,
    description_en TEXT,

    -- Visual
    icon_url VARCHAR(500),
    badge_color VARCHAR(7),  -- HEX color

    -- Criteria
    criteria_type VARCHAR(50) NOT NULL,  -- e.g., "exams_passed", "streak_days", "perfect_score"
    criteria_value INTEGER,  -- Threshold to earn
    points INTEGER DEFAULT 0,  -- XP points awarded

    -- Rarity
    rarity achievement_rarity DEFAULT 'common',

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_achievements_active ON achievements(is_active);
CREATE INDEX idx_achievements_rarity ON achievements(rarity);

-- Student Achievements
CREATE TABLE student_achievements (
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    progress INTEGER DEFAULT 100,  -- Percentage completed

    PRIMARY KEY (student_id, achievement_id)
);

CREATE INDEX idx_student_achievements_student ON student_achievements(student_id);
CREATE INDEX idx_student_achievements_earned ON student_achievements(earned_at DESC);

-- Daily Challenges
CREATE TABLE daily_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_date DATE UNIQUE NOT NULL,

    -- Title (multilingual)
    title_uz VARCHAR(255),
    title_uzk VARCHAR(255),
    title_ru VARCHAR(255),
    title_en VARCHAR(255),

    -- Questions
    question_ids INTEGER[] NOT NULL,  -- Array of question IDs
    reward_points INTEGER DEFAULT 10,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_challenges_date ON daily_challenges(challenge_date DESC);
CREATE INDEX idx_daily_challenges_active ON daily_challenges(is_active);

-- Student Challenge Completions
CREATE TABLE student_challenge_completions (
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    challenge_id UUID NOT NULL REFERENCES daily_challenges(id) ON DELETE CASCADE,
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    score INTEGER NOT NULL,
    time_spent_seconds INTEGER,

    PRIMARY KEY (student_id, challenge_id)
);

CREATE INDEX idx_challenge_completions_student ON student_challenge_completions(student_id);
CREATE INDEX idx_challenge_completions_completed ON student_challenge_completions(completed_at DESC);

-- ==========================================
-- PROGRESS TRACKING
-- ==========================================

-- Student Statistics (Aggregated)
CREATE TABLE student_statistics (
    student_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

    -- Exam stats
    total_exams_taken INTEGER DEFAULT 0,
    total_exams_passed INTEGER DEFAULT 0,
    average_score DECIMAL(5,2) DEFAULT 0,
    highest_score DECIMAL(5,2) DEFAULT 0,
    lowest_score DECIMAL(5,2),

    -- Question stats
    total_questions_answered INTEGER DEFAULT 0,
    total_correct_answers INTEGER DEFAULT 0,
    accuracy_rate DECIMAL(5,2) DEFAULT 0,

    -- Gamification
    total_points INTEGER DEFAULT 0,
    current_streak_days INTEGER DEFAULT 0,
    longest_streak_days INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,

    -- Time tracking
    total_study_time_seconds BIGINT DEFAULT 0,
    last_activity_at TIMESTAMP WITH TIME ZONE,

    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_student_statistics_level ON student_statistics(level DESC);
CREATE INDEX idx_student_statistics_points ON student_statistics(total_points DESC);
CREATE INDEX idx_student_statistics_streak ON student_statistics(current_streak_days DESC);

-- Category Progress (per student, per category)
CREATE TABLE student_category_progress (
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,

    -- Progress
    questions_answered INTEGER DEFAULT 0,
    correct_answers INTEGER DEFAULT 0,
    accuracy_rate DECIMAL(5,2) DEFAULT 0,

    -- Mastery
    mastery_level mastery_level DEFAULT 'beginner',

    -- Timestamps
    first_attempted_at TIMESTAMP WITH TIME ZONE,
    last_attempted_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (student_id, category_id)
);

CREATE INDEX idx_category_progress_student ON student_category_progress(student_id);
CREATE INDEX idx_category_progress_category ON student_category_progress(category_id);
CREATE INDEX idx_category_progress_mastery ON student_category_progress(mastery_level);

-- ==========================================
-- MOBILE APP MANAGEMENT
-- ==========================================

-- App Versions
CREATE TABLE app_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform platform_type NOT NULL,
    version VARCHAR(20) NOT NULL,  -- Semantic versioning: 1.0.0
    build_number INTEGER NOT NULL,

    -- Update control
    is_latest BOOLEAN DEFAULT TRUE,
    is_mandatory BOOLEAN DEFAULT FALSE,  -- Force update
    min_supported_version VARCHAR(20),

    -- Release notes (multilingual)
    release_notes_uz TEXT,
    release_notes_uzk TEXT,
    release_notes_ru TEXT,
    release_notes_en TEXT,

    -- Download
    download_url VARCHAR(500),
    file_size_bytes BIGINT,

    -- Timestamps
    released_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deprecated_at TIMESTAMP WITH TIME ZONE,

    UNIQUE (platform, version)
);

CREATE INDEX idx_app_versions_platform ON app_versions(platform);
CREATE INDEX idx_app_versions_latest ON app_versions(platform, is_latest);

-- ==========================================
-- NOTIFICATIONS
-- ==========================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,  -- NULL for broadcast

    -- Title (multilingual)
    title_uz VARCHAR(255),
    title_uzk VARCHAR(255),
    title_ru VARCHAR(255),
    title_en VARCHAR(255),

    -- Message (multilingual)
    message_uz TEXT,
    message_uzk TEXT,
    message_ru TEXT,
    message_en TEXT,

    -- Type & priority
    notification_type VARCHAR(50) NOT NULL,  -- achievement, exam_result, system, update, etc.
    priority notification_priority DEFAULT 'normal',

    -- Action (deep linking)
    action_type VARCHAR(50),  -- navigate_to_exam, open_achievement, etc.
    action_data JSONB,  -- {exam_id: "...", screen: "ExamDetail"}

    -- Status
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,

    -- Scheduling
    scheduled_for TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,

    -- Expiry
    expires_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE NOT is_read;
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_for) WHERE scheduled_for IS NOT NULL AND sent_at IS NULL;

-- ==========================================
-- AUDIT & LOGGING
-- ==========================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,  -- created_exam, updated_question, deleted_user, etc.
    entity_type VARCHAR(50),  -- exam, question, user, etc.
    entity_id VARCHAR(255),

    -- Changes (before/after)
    changes JSONB,  -- {before: {...}, after: {...}}

    -- Request metadata
    ip_address INET,
    user_agent TEXT,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);

-- ==========================================
-- SEED DATA - PRIVILEGES
-- ==========================================

INSERT INTO privileges (name, group_name, description) VALUES
-- User management
('users.view', 'users', 'View users'),
('users.create', 'users', 'Create new users'),
('users.update', 'users', 'Update user information'),
('users.delete', 'users', 'Delete users'),
('users.verify', 'users', 'Verify user accounts'),

-- Role management
('roles.view', 'roles', 'View roles'),
('roles.create', 'roles', 'Create new roles'),
('roles.update', 'roles', 'Update roles'),
('roles.delete', 'roles', 'Delete roles'),

-- Question management
('questions.view', 'questions', 'View questions'),
('questions.create', 'questions', 'Create new questions'),
('questions.update', 'questions', 'Update questions'),
('questions.delete', 'questions', 'Delete questions'),
('questions.import', 'questions', 'Import questions from files'),

-- Category management
('categories.view', 'categories', 'View categories'),
('categories.create', 'categories', 'Create new categories'),
('categories.update', 'categories', 'Update categories'),
('categories.delete', 'categories', 'Delete categories'),

-- Exam management
('exams.view', 'exams', 'View exam templates'),
('exams.create', 'exams', 'Create exam templates'),
('exams.update', 'exams', 'Update exam templates'),
('exams.delete', 'exams', 'Delete exam templates'),
('exams.take', 'exams', 'Take exams'),
('exams.view_results', 'exams', 'View exam results'),

-- Achievement management
('achievements.view', 'achievements', 'View achievements'),
('achievements.create', 'achievements', 'Create achievements'),
('achievements.update', 'achievements', 'Update achievements'),
('achievements.delete', 'achievements', 'Delete achievements'),
('achievements.award', 'achievements', 'Manually award achievements'),

-- Statistics & Reports
('statistics.view_own', 'statistics', 'View own statistics'),
('statistics.view_all', 'statistics', 'View all user statistics'),
('reports.generate', 'reports', 'Generate reports'),

-- System administration
('admin.view_logs', 'admin', 'View audit logs'),
('admin.manage_app_versions', 'admin', 'Manage mobile app versions'),
('admin.send_notifications', 'admin', 'Send push notifications'),
('admin.system_settings', 'admin', 'Manage system settings');

-- ==========================================
-- SEED DATA - ROLES
-- ==========================================

INSERT INTO roles (id, name, description, is_system) VALUES
('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'System super administrator with all privileges', TRUE),
('00000000-0000-0000-0000-000000000002', 'ADMIN', 'Platform administrator', TRUE),
('00000000-0000-0000-0000-000000000003', 'TEACHER', 'Teacher/Instructor role', TRUE),
('00000000-0000-0000-0000-000000000004', 'STUDENT', 'Student role', TRUE);

-- ==========================================
-- SEED DATA - ROLE PRIVILEGES
-- ==========================================

-- SUPER_ADMIN: All privileges
INSERT INTO role_privileges (role_id, privilege)
SELECT '00000000-0000-0000-0000-000000000001', name FROM privileges;

-- ADMIN: Most privileges except some system-level
INSERT INTO role_privileges (role_id, privilege)
SELECT '00000000-0000-0000-0000-000000000002', name FROM privileges
WHERE name NOT IN ('admin.system_settings');

-- TEACHER: Question and exam management
INSERT INTO role_privileges (role_id, privilege) VALUES
('00000000-0000-0000-0000-000000000003', 'questions.view'),
('00000000-0000-0000-0000-000000000003', 'questions.create'),
('00000000-0000-0000-0000-000000000003', 'questions.update'),
('00000000-0000-0000-0000-000000000003', 'categories.view'),
('00000000-0000-0000-0000-000000000003', 'exams.view'),
('00000000-0000-0000-0000-000000000003', 'exams.create'),
('00000000-0000-0000-0000-000000000003', 'exams.view_results'),
('00000000-0000-0000-0000-000000000003', 'statistics.view_all'),
('00000000-0000-0000-0000-000000000003', 'reports.generate');

-- STUDENT: Basic student privileges
INSERT INTO role_privileges (role_id, privilege) VALUES
('00000000-0000-0000-0000-000000000004', 'exams.view'),
('00000000-0000-0000-0000-000000000004', 'exams.take'),
('00000000-0000-0000-0000-000000000004', 'exams.view_results'),
('00000000-0000-0000-0000-000000000004', 'statistics.view_own'),
('00000000-0000-0000-0000-000000000004', 'achievements.view'),
('00000000-0000-0000-0000-000000000004', 'questions.view'),
('00000000-0000-0000-0000-000000000004', 'categories.view');

-- ==========================================
-- SEED DATA - DEFAULT ADMIN USER
-- ==========================================

-- Password: admin123 (SCrypt hashed)
-- NOTE: This should be changed after first login in production
INSERT INTO users (id, email, password, first_name, last_name, phone, role_id, status, email_verified, phone_verified) VALUES
('00000000-0000-0000-0000-000000000001',
 'admin@avtotest.uz',
 '$s0$e0801$epIxT/h6HuwSEH/3SwepLA==$bkzpvrLvQu/v8hPwP2qGLFn3irgQGOGV7iGJa8eC0hk=',  -- admin123
 'System',
 'Administrator',
 '+998901234567',
 '00000000-0000-0000-0000-000000000001',
 'active',
 TRUE,
 TRUE);

-- ==========================================
-- SEED DATA - DEFAULT CATEGORIES
-- ==========================================

INSERT INTO categories (name_uz, name_ru, name_en, description_uz, order_index, is_active) VALUES
('Yo''l belgilari', 'Дорожные знаки', 'Road Signs', 'Yo''l harakatining belgilari va ularning ma''nolari', 1, TRUE),
('Svetoforlar', 'Светофоры', 'Traffic Lights', 'Svetofor signallari va ularning talablari', 2, TRUE),
('Yo''l nishonlari', 'Дорожная разметка', 'Road Markings', 'Yo''l nishonlari va ularning ma''nolari', 3, TRUE),
('Harakatlanish qoidalari', 'Правила движения', 'Traffic Rules', 'Asosiy yo''l harakati qoidalari', 4, TRUE),
('Tortishni amalga oshirish', 'Буксировка', 'Towing', 'Transport vositalarini tortish qoidalari', 5, TRUE),
('Birinchi tibbiy yordam', 'Первая медицинская помощь', 'First Aid', 'Yo''l-transport hodisalarida birinchi yordam', 6, TRUE);

-- ==========================================
-- TRIGGERS
-- ==========================================

-- Update updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_questions_updated_at BEFORE UPDATE ON questions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exam_templates_updated_at BEFORE UPDATE ON exam_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_student_statistics_updated_at BEFORE UPDATE ON student_statistics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_student_category_progress_updated_at BEFORE UPDATE ON student_category_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- VIEWS FOR COMMON QUERIES
-- ==========================================

-- Leaderboard view
CREATE VIEW leaderboard AS
SELECT
    u.id,
    u.first_name,
    u.last_name,
    ss.total_points,
    ss.level,
    ss.total_exams_passed,
    ss.average_score,
    ss.current_streak_days,
    ROW_NUMBER() OVER (ORDER BY ss.total_points DESC, ss.average_score DESC) as rank
FROM users u
INNER JOIN student_statistics ss ON u.id = ss.student_id
WHERE u.deleted_at IS NULL AND u.status = 'active'
ORDER BY ss.total_points DESC, ss.average_score DESC;

-- Active exam attempts view
CREATE VIEW active_exam_attempts AS
SELECT
    sea.id,
    sea.student_id,
    u.first_name,
    u.last_name,
    et.title_uz as exam_title,
    sea.started_at,
    sea.duration_seconds,
    et.duration_minutes * 60 - EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - sea.started_at))::INTEGER as remaining_seconds,
    sea.status
FROM student_exam_attempts sea
INNER JOIN users u ON sea.student_id = u.id
INNER JOIN exam_templates et ON sea.exam_template_id = et.id
WHERE sea.status = 'in_progress';

-- Question statistics view
CREATE VIEW question_statistics AS
SELECT
    q.id,
    q.question_uz,
    q.category_id,
    c.name_uz as category_name,
    COUNT(sa.id) as times_answered,
    COUNT(CASE WHEN sa.is_correct THEN 1 END) as correct_count,
    ROUND(COUNT(CASE WHEN sa.is_correct THEN 1 END)::DECIMAL / NULLIF(COUNT(sa.id), 0) * 100, 2) as correct_percentage,
    AVG(sa.time_spent_seconds) as avg_time_seconds
FROM questions q
LEFT JOIN student_answers sa ON q.id = sa.question_id
LEFT JOIN categories c ON q.category_id = c.id
WHERE q.is_active = TRUE
GROUP BY q.id, q.question_uz, q.category_id, c.name_uz;