-- ============================================================================
-- V3: Adiciona sistema de descontos para documentação atrasada
-- ============================================================================

-- 1. Novos campos na tabela de solicitações
ALTER TABLE solicitacoes_documento ADD COLUMN IF NOT EXISTS prazo_entrega TIMESTAMP;
ALTER TABLE solicitacoes_documento ADD COLUMN IF NOT EXISTS valor_desconto NUMERIC(10,2);
ALTER TABLE solicitacoes_documento ADD COLUMN IF NOT EXISTS percentual_desconto NUMERIC(5,2);
ALTER TABLE solicitacoes_documento ADD COLUMN IF NOT EXISTS desconto_renegociado BOOLEAN DEFAULT FALSE;
ALTER TABLE solicitacoes_documento ADD COLUMN IF NOT EXISTS valor_final NUMERIC(10,2);

-- 2. Tabela de configuração
CREATE TABLE IF NOT EXISTS configuracao_documentacao (
    id BIGSERIAL PRIMARY KEY,
    chave VARCHAR(100) NOT NULL UNIQUE,
    valor VARCHAR(255) NOT NULL,
    atualizado_em TIMESTAMP
);

-- 3. Configuração padrão: desconto desativado
INSERT INTO configuracao_documentacao (chave, valor, atualizado_em)
VALUES ('DESCONTO_ATIVO', 'false', NOW())
ON CONFLICT (chave) DO NOTHING;
