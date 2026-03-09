CREATE TABLE user_activities (
                                 id UUID NOT NULL,
                                 user_id UUID NOT NULL,
                                 action VARCHAR(100) NOT NULL,
                                 details TEXT,
                                 ip_address VARCHAR(45),
                                 user_agent VARCHAR(500),
                                 created_at TIMESTAMP NOT NULL,
                                 CONSTRAINT pk_user_activities PRIMARY KEY (id),
                                 CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_user_created ON user_activities (user_id, created_at DESC);
CREATE INDEX idx_activity_action ON user_activities (action);