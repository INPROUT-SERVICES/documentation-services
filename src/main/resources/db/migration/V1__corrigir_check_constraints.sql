-- =============================================================================
-- V1: Corrigir CHECK constraints para incluir novos status
-- O Hibernate ddl-auto: update NUNCA atualiza constraints existentes.
-- Esta migration resolve isso de forma versionada e rastreável.
-- =============================================================================

-- 1. Constraint de STATUS na tabela solicitacoes_documento
--    Adiciona: RECUSADO
ALTER TABLE solicitacoes_documento
    DROP CONSTRAINT IF EXISTS solicitacoes_documento_status_check;

ALTER TABLE solicitacoes_documento
    ADD CONSTRAINT solicitacoes_documento_status_check
    CHECK (status::text = ANY (ARRAY[
        'AGUARDANDO_RECEBIMENTO',
        'RECEBIDO',
        'FINALIZADO',
        'CANCELADO',
        'RECUSADO'
    ]::text[]));

-- 2. Constraint de TIPO_EVENTO na tabela solicitacao_documento_eventos
--    Adiciona: RECUSADO, RESOLICITADO
ALTER TABLE solicitacao_documento_eventos
    DROP CONSTRAINT IF EXISTS solicitacao_documento_eventos_tipo_evento_check;

ALTER TABLE solicitacao_documento_eventos
    ADD CONSTRAINT solicitacao_documento_eventos_tipo_evento_check
    CHECK (tipo_evento::text = ANY (ARRAY[
        'CRIADA',
        'ATRIBUIDO',
        'MARCADO_RECEBIDO',
        'FINALIZADO',
        'CANCELADO',
        'COMENTARIO',
        'RECUSADO',
        'RESOLICITADO'
    ]::text[]));

-- 3. Constraint de STATUS_NOVO na tabela solicitacao_documento_eventos
--    Adiciona: RECUSADO
ALTER TABLE solicitacao_documento_eventos
    DROP CONSTRAINT IF EXISTS solicitacao_documento_eventos_status_novo_check;

ALTER TABLE solicitacao_documento_eventos
    ADD CONSTRAINT solicitacao_documento_eventos_status_novo_check
    CHECK (status_novo::text = ANY (ARRAY[
        'AGUARDANDO_RECEBIMENTO',
        'RECEBIDO',
        'FINALIZADO',
        'CANCELADO',
        'RECUSADO'
    ]::text[]));

-- 4. Constraint de STATUS_ANTERIOR na tabela solicitacao_documento_eventos
--    Adiciona: RECUSADO
ALTER TABLE solicitacao_documento_eventos
    DROP CONSTRAINT IF EXISTS solicitacao_documento_eventos_status_anterior_check;

ALTER TABLE solicitacao_documento_eventos
    ADD CONSTRAINT solicitacao_documento_eventos_status_anterior_check
    CHECK (status_anterior::text = ANY (ARRAY[
        'AGUARDANDO_RECEBIMENTO',
        'RECEBIDO',
        'FINALIZADO',
        'CANCELADO',
        'RECUSADO'
    ]::text[]));
