create table coupons(
    id bigserial primary key,
    code varchar(50) not null unique ,
    discount DOUBLE PRECISION NOT NULL,
    max_discount DOUBLE PRECISION NOT NULL,
    created_at timestamp NOT NULL,
    modified_at timestamp NOT NULL,
    created_by BIGINT,
    modified_by BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
)