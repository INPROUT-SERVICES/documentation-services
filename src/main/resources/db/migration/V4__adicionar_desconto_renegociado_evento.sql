-- =============================================================================
-- V4: Adicionar DESCONTO_RENEGOCIADO nas CHECK constraints de tipo_evento
-- =============================================================================

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
        'EDITADO',
        'DESCONTO_RENEGOCIADO'
    ]::text[]));
