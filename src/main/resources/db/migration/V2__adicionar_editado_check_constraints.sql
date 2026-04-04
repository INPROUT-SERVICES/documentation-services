-- =============================================================================
-- V2: Adicionar EDITADO nas CHECK constraints
-- Necessário para suportar a funcionalidade de edição de solicitações recusadas.
-- =============================================================================

-- 1. Constraint de TIPO_EVENTO: adiciona EDITADO
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
        'RESOLICITADO',
        'EDITADO'
    ]::text[]));
