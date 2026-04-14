CREATE TABLE IF NOT EXISTS agent (
    id       SERIAL PRIMARY KEY,
    "name"       VARCHAR(50)  NOT NULL,
    "chatId"     VARCHAR(255) NOT NULL,
    "text"       VARCHAR(255) NOT NULL,
    repository   VARCHAR(255) NOT NULL,
    ref          VARCHAR(255) NOT NULL,
    "branchName" VARCHAR(255) NOT NULL,
    "prUri"      VARCHAR(255),
    "jobId"      VARCHAR(255),
    provider     VARCHAR(255) NOT NULL
);
