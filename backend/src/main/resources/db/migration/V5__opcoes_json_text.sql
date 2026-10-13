-- V5: Adequar coluna opcoes_json para TEXT compatível com driver JDBC/JPA
ALTER TABLE questao ALTER COLUMN opcoes_json TYPE TEXT;
