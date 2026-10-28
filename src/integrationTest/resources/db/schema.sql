--
-- PostgreSQL database dump
--

\restrict dX4IfBh5ZWbpzqISQnjk9UvU5saqtt9B1dC89jFUC91SUiZYqSqwCiUyCXjIsuL

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: zipbob_app; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA zipbob_app;


--
-- Name: codename_f(character varying); Type: FUNCTION; Schema: zipbob_app; Owner: -
--

CREATE FUNCTION zipbob_app.codename_f(pr_code_id character varying) RETURNS character varying
    LANGUAGE sql
    AS $$
    SELECT code_name
    FROM zipbob_app.code_mng
    WHERE code_id = pr_code_id
      AND use_yn = 'Y';
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: code_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.code_mng (
    code_id character varying(50) NOT NULL,
    code_name character varying(50) NOT NULL,
    code_parent character varying(50),
    code_depth integer,
    use_yn character(1) DEFAULT 'Y'::bpchar,
    created_id character varying(50),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_id character varying(50),
    updated_dt timestamp without time zone
);


--
-- Name: TABLE code_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.code_mng IS '코드관리';


--
-- Name: COLUMN code_mng.code_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.code_id IS '코드ID';


--
-- Name: COLUMN code_mng.code_name; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.code_name IS '코드명';


--
-- Name: COLUMN code_mng.code_parent; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.code_parent IS '코드부모';


--
-- Name: COLUMN code_mng.code_depth; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.code_depth IS 'depth';


--
-- Name: COLUMN code_mng.use_yn; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.use_yn IS '사용여부';


--
-- Name: COLUMN code_mng.created_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.created_id IS '작성자';


--
-- Name: COLUMN code_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.crated_dt IS '작성일';


--
-- Name: COLUMN code_mng.updated_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.updated_id IS '수정자';


--
-- Name: COLUMN code_mng.updated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.code_mng.updated_dt IS '수정일';


--
-- Name: email_verification_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.email_verification_mng (
    email_verification_id bigint NOT NULL,
    email character varying(50) NOT NULL,
    email_adr character varying(50) NOT NULL,
    purpose character varying(30) NOT NULL,
    verification_code_hash character varying(255) NOT NULL,
    expires_dt timestamp without time zone NOT NULL,
    verified_dt timestamp without time zone,
    verification_token_hash character varying(255),
    token_expires_dt timestamp without time zone,
    used_dt timestamp without time zone,
    attempt_count integer DEFAULT 0 NOT NULL,
    created_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_email_verification_attempt_count CHECK ((attempt_count >= 0)),
    CONSTRAINT ck_email_verification_purpose CHECK (((purpose)::text = ANY ((ARRAY['SIGNUP'::character varying, 'FIND_ID'::character varying, 'RESET_PASSWORD'::character varying])::text[])))
);


--
-- Name: TABLE email_verification_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.email_verification_mng IS '이메일 인증관리';


--
-- Name: COLUMN email_verification_mng.email_verification_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.email_verification_id IS '이메일인증ID';


--
-- Name: COLUMN email_verification_mng.email; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.email IS '이메일 아이디(@ 앞부분)';


--
-- Name: COLUMN email_verification_mng.email_adr; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.email_adr IS '이메일 도메인(@ 뒷부분)';


--
-- Name: COLUMN email_verification_mng.purpose; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.purpose IS '인증 목적(SIGNUP: 회원가입, FIND_ID: 아이디 찾기, RESET_PASSWORD: 비밀번호 재설정)';


--
-- Name: COLUMN email_verification_mng.verification_code_hash; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.verification_code_hash IS '인증번호 해시값';


--
-- Name: COLUMN email_verification_mng.expires_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.expires_dt IS '인증번호 만료일시';


--
-- Name: COLUMN email_verification_mng.verified_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.verified_dt IS '인증 완료일시';


--
-- Name: COLUMN email_verification_mng.verification_token_hash; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.verification_token_hash IS '인증 완료 후 발급한 인증 증명 토큰의 해시값';


--
-- Name: COLUMN email_verification_mng.token_expires_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.token_expires_dt IS '인증 증명 토큰 만료일시';


--
-- Name: COLUMN email_verification_mng.used_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.used_dt IS '인증 증명 사용 완료일시';


--
-- Name: COLUMN email_verification_mng.attempt_count; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.attempt_count IS '인증번호 입력 실패 횟수';


--
-- Name: COLUMN email_verification_mng.created_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.email_verification_mng.created_dt IS '작성일';


--
-- Name: email_verification_mng_email_verification_id_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

ALTER TABLE zipbob_app.email_verification_mng ALTER COLUMN email_verification_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME zipbob_app.email_verification_mng_email_verification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: file_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.file_mng (
    file_id integer NOT NULL,
    menu_id integer NOT NULL,
    file_path character varying(255) NOT NULL,
    file_size integer,
    file_save_name character varying(255) NOT NULL,
    file_name character varying(100) NOT NULL,
    use_yn character(1) DEFAULT 'Y'::bpchar,
    created_id character varying(50),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_id character varying(50),
    updated_dt timestamp without time zone
);


--
-- Name: TABLE file_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.file_mng IS '파일관리';


--
-- Name: COLUMN file_mng.file_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.file_id IS '파일ID';


--
-- Name: COLUMN file_mng.menu_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.menu_id IS '메뉴ID';


--
-- Name: COLUMN file_mng.file_path; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.file_path IS '파일경로';


--
-- Name: COLUMN file_mng.file_size; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.file_size IS '파일크기';


--
-- Name: COLUMN file_mng.file_save_name; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.file_save_name IS '파일저장명';


--
-- Name: COLUMN file_mng.file_name; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.file_name IS '파일명';


--
-- Name: COLUMN file_mng.use_yn; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.use_yn IS '사용여부';


--
-- Name: COLUMN file_mng.created_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.created_id IS '작성자';


--
-- Name: COLUMN file_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.crated_dt IS '작성일';


--
-- Name: COLUMN file_mng.updated_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.updated_id IS '수정자';


--
-- Name: COLUMN file_mng.updated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.file_mng.updated_dt IS '수정일';


--
-- Name: file_mng_file_id_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

CREATE SEQUENCE zipbob_app.file_mng_file_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: file_mng_file_id_seq; Type: SEQUENCE OWNED BY; Schema: zipbob_app; Owner: -
--

ALTER SEQUENCE zipbob_app.file_mng_file_id_seq OWNED BY zipbob_app.file_mng.file_id;


--
-- Name: like_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.like_mng (
    menu_id integer NOT NULL,
    created_id character varying(50),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    user_id character varying(50) NOT NULL
);


--
-- Name: TABLE like_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.like_mng IS '즐겨찾기관리';


--
-- Name: COLUMN like_mng.menu_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.like_mng.menu_id IS '메뉴ID';


--
-- Name: COLUMN like_mng.created_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.like_mng.created_id IS '작성자';


--
-- Name: COLUMN like_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.like_mng.crated_dt IS '작성일';


--
-- Name: menu_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.menu_mng (
    menu_id integer NOT NULL,
    menu_name character varying(50) NOT NULL,
    category character varying(50),
    description character varying(100),
    servings integer,
    cook_time integer,
    cook_minute integer,
    ingredients text,
    steps text,
    created_id character varying(50),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_id character varying(50),
    updated_dt timestamp without time zone,
    difficulty_lvl character varying(50)
);


--
-- Name: TABLE menu_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.menu_mng IS '메뉴관리';


--
-- Name: COLUMN menu_mng.menu_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.menu_id IS '메뉴ID';


--
-- Name: COLUMN menu_mng.menu_name; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.menu_name IS '메뉴명';


--
-- Name: COLUMN menu_mng.category; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.category IS '카테고리';


--
-- Name: COLUMN menu_mng.description; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.description IS '한줄설명';


--
-- Name: COLUMN menu_mng.servings; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.servings IS '기준인분';


--
-- Name: COLUMN menu_mng.cook_time; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.cook_time IS '조리 시각';


--
-- Name: COLUMN menu_mng.cook_minute; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.cook_minute IS '조리 시분';


--
-- Name: COLUMN menu_mng.ingredients; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.ingredients IS '재료';


--
-- Name: COLUMN menu_mng.steps; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.steps IS '조리순서';


--
-- Name: COLUMN menu_mng.created_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.created_id IS '작성자';


--
-- Name: COLUMN menu_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.crated_dt IS '작성일';


--
-- Name: COLUMN menu_mng.updated_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.updated_id IS '수정자';


--
-- Name: COLUMN menu_mng.updated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.updated_dt IS '수정일';


--
-- Name: COLUMN menu_mng.difficulty_lvl; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.menu_mng.difficulty_lvl IS '난이도';


--
-- Name: menu_mng_menu_id_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

CREATE SEQUENCE zipbob_app.menu_mng_menu_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: menu_mng_menu_id_seq; Type: SEQUENCE OWNED BY; Schema: zipbob_app; Owner: -
--

ALTER SEQUENCE zipbob_app.menu_mng_menu_id_seq OWNED BY zipbob_app.menu_mng.menu_id;


--
-- Name: recent_recipe_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.recent_recipe_mng (
    recent_recipe_id integer NOT NULL,
    menu_id integer NOT NULL,
    viewed_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id character varying(50) NOT NULL
);


--
-- Name: TABLE recent_recipe_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.recent_recipe_mng IS '최근 본 레시피 관리';


--
-- Name: COLUMN recent_recipe_mng.recent_recipe_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.recent_recipe_mng.recent_recipe_id IS '최근 본 레시피 기록 ID';


--
-- Name: COLUMN recent_recipe_mng.menu_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.recent_recipe_mng.menu_id IS '메뉴 ID';


--
-- Name: COLUMN recent_recipe_mng.viewed_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.recent_recipe_mng.viewed_dt IS '마지막 조회 일시';


--
-- Name: recent_recipe_mng_recent_recipe_id_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

CREATE SEQUENCE zipbob_app.recent_recipe_mng_recent_recipe_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recent_recipe_mng_recent_recipe_id_seq; Type: SEQUENCE OWNED BY; Schema: zipbob_app; Owner: -
--

ALTER SEQUENCE zipbob_app.recent_recipe_mng_recent_recipe_id_seq OWNED BY zipbob_app.recent_recipe_mng.recent_recipe_id;


--
-- Name: tag_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.tag_mng (
    tag_seq integer NOT NULL,
    menu_id integer NOT NULL,
    code_id character varying(50) NOT NULL,
    use_yn character(1) DEFAULT 'Y'::bpchar,
    created_id character varying(50),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_id character varying(50),
    updated_dt timestamp without time zone
);


--
-- Name: TABLE tag_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.tag_mng IS '태그관리';


--
-- Name: COLUMN tag_mng.tag_seq; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.tag_seq IS '태그정렬순번';


--
-- Name: COLUMN tag_mng.menu_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.menu_id IS '메뉴ID';


--
-- Name: COLUMN tag_mng.code_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.code_id IS '코드ID';


--
-- Name: COLUMN tag_mng.use_yn; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.use_yn IS '사용여부';


--
-- Name: COLUMN tag_mng.created_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.created_id IS '작성자';


--
-- Name: COLUMN tag_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.crated_dt IS '작성일';


--
-- Name: COLUMN tag_mng.updated_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.updated_id IS '수정자';


--
-- Name: COLUMN tag_mng.updated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.tag_mng.updated_dt IS '수정일';


--
-- Name: tag_mng_tag_seq_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

CREATE SEQUENCE zipbob_app.tag_mng_tag_seq_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tag_mng_tag_seq_seq; Type: SEQUENCE OWNED BY; Schema: zipbob_app; Owner: -
--

ALTER SEQUENCE zipbob_app.tag_mng_tag_seq_seq OWNED BY zipbob_app.tag_mng.tag_seq;


--
-- Name: test; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.test (
    col1 character varying(10),
    col2 character varying(10),
    col3 character varying(10)
);


--
-- Name: user_device_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.user_device_mng (
    user_device_id integer NOT NULL,
    device_id character varying(255) NOT NULL,
    fcm_token text,
    os_type character varying(20),
    os_version character varying(20),
    device_model character varying(100),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_dt timestamp without time zone
);


--
-- Name: TABLE user_device_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.user_device_mng IS '사용자 기기관리';


--
-- Name: COLUMN user_device_mng.user_device_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.user_device_id IS '사용자기기ID';


--
-- Name: COLUMN user_device_mng.device_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.device_id IS '기기ID';


--
-- Name: COLUMN user_device_mng.fcm_token; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.fcm_token IS '푸시 알림용 토큰';


--
-- Name: COLUMN user_device_mng.os_type; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.os_type IS 'os 구분';


--
-- Name: COLUMN user_device_mng.os_version; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.os_version IS 'os 버전';


--
-- Name: COLUMN user_device_mng.device_model; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.device_model IS '기기 모델명';


--
-- Name: COLUMN user_device_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.crated_dt IS '작성일';


--
-- Name: COLUMN user_device_mng.updated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_device_mng.updated_dt IS '수정일';


--
-- Name: user_device_mng_user_device_id_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

CREATE SEQUENCE zipbob_app.user_device_mng_user_device_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_device_mng_user_device_id_seq; Type: SEQUENCE OWNED BY; Schema: zipbob_app; Owner: -
--

ALTER SEQUENCE zipbob_app.user_device_mng_user_device_id_seq OWNED BY zipbob_app.user_device_mng.user_device_id;


--
-- Name: user_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.user_mng (
    user_id character varying(50) NOT NULL,
    user_device_id integer,
    user_pw character varying(255) NOT NULL,
    user_name character varying(50) NOT NULL,
    email character varying(50),
    email_adr character varying(50),
    created_id character varying(50),
    crated_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_id character varying(50),
    updated_dt timestamp without time zone,
    user_role character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    CONSTRAINT ck_user_mng_user_role CHECK (((user_role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: TABLE user_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.user_mng IS '회원관리';


--
-- Name: COLUMN user_mng.user_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.user_id IS '회원ID';


--
-- Name: COLUMN user_mng.user_device_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.user_device_id IS '사용자기기ID';


--
-- Name: COLUMN user_mng.user_pw; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.user_pw IS '비밀번호';


--
-- Name: COLUMN user_mng.user_name; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.user_name IS '회원명';


--
-- Name: COLUMN user_mng.email; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.email IS '이메일';


--
-- Name: COLUMN user_mng.email_adr; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.email_adr IS '이메일주소';


--
-- Name: COLUMN user_mng.created_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.created_id IS '작성자';


--
-- Name: COLUMN user_mng.crated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.crated_dt IS '작성일';


--
-- Name: COLUMN user_mng.updated_id; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.updated_id IS '수정자';


--
-- Name: COLUMN user_mng.updated_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_mng.updated_dt IS '수정일';


--
-- Name: user_refresh_token_mng; Type: TABLE; Schema: zipbob_app; Owner: -
--

CREATE TABLE zipbob_app.user_refresh_token_mng (
    refresh_token_id bigint NOT NULL,
    user_id character varying(50) NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_dt timestamp with time zone NOT NULL,
    created_dt timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_refresh_token_expiry CHECK ((expires_dt > created_dt))
);


--
-- Name: TABLE user_refresh_token_mng; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON TABLE zipbob_app.user_refresh_token_mng IS '회원 로그인 유지 및 액세스 토큰 재발급 관리';


--
-- Name: COLUMN user_refresh_token_mng.token_hash; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_refresh_token_mng.token_hash IS '리프레시 토큰의 SHA-256 해시, 원문 저장 금지';


--
-- Name: COLUMN user_refresh_token_mng.expires_dt; Type: COMMENT; Schema: zipbob_app; Owner: -
--

COMMENT ON COLUMN zipbob_app.user_refresh_token_mng.expires_dt IS '리프레시 토큰 만료 일시';


--
-- Name: user_refresh_token_mng_refresh_token_id_seq; Type: SEQUENCE; Schema: zipbob_app; Owner: -
--

ALTER TABLE zipbob_app.user_refresh_token_mng ALTER COLUMN refresh_token_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME zipbob_app.user_refresh_token_mng_refresh_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: file_mng file_id; Type: DEFAULT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.file_mng ALTER COLUMN file_id SET DEFAULT nextval('zipbob_app.file_mng_file_id_seq'::regclass);


--
-- Name: menu_mng menu_id; Type: DEFAULT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.menu_mng ALTER COLUMN menu_id SET DEFAULT nextval('zipbob_app.menu_mng_menu_id_seq'::regclass);


--
-- Name: recent_recipe_mng recent_recipe_id; Type: DEFAULT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.recent_recipe_mng ALTER COLUMN recent_recipe_id SET DEFAULT nextval('zipbob_app.recent_recipe_mng_recent_recipe_id_seq'::regclass);


--
-- Name: tag_mng tag_seq; Type: DEFAULT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.tag_mng ALTER COLUMN tag_seq SET DEFAULT nextval('zipbob_app.tag_mng_tag_seq_seq'::regclass);


--
-- Name: user_device_mng user_device_id; Type: DEFAULT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_device_mng ALTER COLUMN user_device_id SET DEFAULT nextval('zipbob_app.user_device_mng_user_device_id_seq'::regclass);


--
-- Name: code_mng code_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.code_mng
    ADD CONSTRAINT code_mng_pkey PRIMARY KEY (code_id);


--
-- Name: email_verification_mng email_verification_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.email_verification_mng
    ADD CONSTRAINT email_verification_mng_pkey PRIMARY KEY (email_verification_id);


--
-- Name: file_mng file_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.file_mng
    ADD CONSTRAINT file_mng_pkey PRIMARY KEY (file_id);


--
-- Name: like_mng like_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.like_mng
    ADD CONSTRAINT like_mng_pkey PRIMARY KEY (user_id, menu_id);


--
-- Name: menu_mng menu_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.menu_mng
    ADD CONSTRAINT menu_mng_pkey PRIMARY KEY (menu_id);


--
-- Name: recent_recipe_mng recent_recipe_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.recent_recipe_mng
    ADD CONSTRAINT recent_recipe_mng_pkey PRIMARY KEY (recent_recipe_id);


--
-- Name: tag_mng tag_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.tag_mng
    ADD CONSTRAINT tag_mng_pkey PRIMARY KEY (tag_seq);


--
-- Name: email_verification_mng uq_email_verification_token_hash; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.email_verification_mng
    ADD CONSTRAINT uq_email_verification_token_hash UNIQUE (verification_token_hash);


--
-- Name: recent_recipe_mng uq_recent_recipe_user_menu; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.recent_recipe_mng
    ADD CONSTRAINT uq_recent_recipe_user_menu UNIQUE (user_id, menu_id);


--
-- Name: user_device_mng uq_user_device_mng_device_id; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_device_mng
    ADD CONSTRAINT uq_user_device_mng_device_id UNIQUE (device_id);


--
-- Name: user_mng uq_user_email; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_mng
    ADD CONSTRAINT uq_user_email UNIQUE (email, email_adr);


--
-- Name: user_mng uq_user_name; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_mng
    ADD CONSTRAINT uq_user_name UNIQUE (user_name);


--
-- Name: user_refresh_token_mng uq_user_refresh_token_hash; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_refresh_token_mng
    ADD CONSTRAINT uq_user_refresh_token_hash UNIQUE (token_hash);


--
-- Name: user_device_mng user_device_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_device_mng
    ADD CONSTRAINT user_device_mng_pkey PRIMARY KEY (user_device_id);


--
-- Name: user_mng user_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_mng
    ADD CONSTRAINT user_mng_pkey PRIMARY KEY (user_id);


--
-- Name: user_refresh_token_mng user_refresh_token_mng_pkey; Type: CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_refresh_token_mng
    ADD CONSTRAINT user_refresh_token_mng_pkey PRIMARY KEY (refresh_token_id);


--
-- Name: idx_email_verification_email_purpose_created; Type: INDEX; Schema: zipbob_app; Owner: -
--

CREATE INDEX idx_email_verification_email_purpose_created ON zipbob_app.email_verification_mng USING btree (email, email_adr, purpose, created_dt DESC);


--
-- Name: idx_email_verification_expires_dt; Type: INDEX; Schema: zipbob_app; Owner: -
--

CREATE INDEX idx_email_verification_expires_dt ON zipbob_app.email_verification_mng USING btree (expires_dt);


--
-- Name: idx_refresh_token_expires_dt; Type: INDEX; Schema: zipbob_app; Owner: -
--

CREATE INDEX idx_refresh_token_expires_dt ON zipbob_app.user_refresh_token_mng USING btree (expires_dt);


--
-- Name: idx_refresh_token_user_id; Type: INDEX; Schema: zipbob_app; Owner: -
--

CREATE INDEX idx_refresh_token_user_id ON zipbob_app.user_refresh_token_mng USING btree (user_id);


--
-- Name: ix_like_user_created; Type: INDEX; Schema: zipbob_app; Owner: -
--

CREATE INDEX ix_like_user_created ON zipbob_app.like_mng USING btree (user_id, crated_dt DESC, menu_id);


--
-- Name: ix_recent_recipe_user_viewed; Type: INDEX; Schema: zipbob_app; Owner: -
--

CREATE INDEX ix_recent_recipe_user_viewed ON zipbob_app.recent_recipe_mng USING btree (user_id, viewed_dt DESC, recent_recipe_id DESC);


--
-- Name: file_mng fk_file_menu; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.file_mng
    ADD CONSTRAINT fk_file_menu FOREIGN KEY (menu_id) REFERENCES zipbob_app.menu_mng(menu_id) ON DELETE SET NULL;


--
-- Name: like_mng fk_like_menu; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.like_mng
    ADD CONSTRAINT fk_like_menu FOREIGN KEY (menu_id) REFERENCES zipbob_app.menu_mng(menu_id) ON DELETE CASCADE;


--
-- Name: like_mng fk_like_user; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.like_mng
    ADD CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES zipbob_app.user_mng(user_id) ON DELETE CASCADE;


--
-- Name: recent_recipe_mng fk_recent_recipe_menu; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.recent_recipe_mng
    ADD CONSTRAINT fk_recent_recipe_menu FOREIGN KEY (menu_id) REFERENCES zipbob_app.menu_mng(menu_id);


--
-- Name: recent_recipe_mng fk_recent_recipe_user; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.recent_recipe_mng
    ADD CONSTRAINT fk_recent_recipe_user FOREIGN KEY (user_id) REFERENCES zipbob_app.user_mng(user_id) ON DELETE CASCADE;


--
-- Name: user_refresh_token_mng fk_refresh_token_user; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_refresh_token_mng
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES zipbob_app.user_mng(user_id) ON DELETE CASCADE;


--
-- Name: tag_mng fk_tag_code; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.tag_mng
    ADD CONSTRAINT fk_tag_code FOREIGN KEY (code_id) REFERENCES zipbob_app.code_mng(code_id) ON DELETE CASCADE;


--
-- Name: tag_mng fk_tag_menu; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.tag_mng
    ADD CONSTRAINT fk_tag_menu FOREIGN KEY (menu_id) REFERENCES zipbob_app.menu_mng(menu_id) ON DELETE CASCADE;


--
-- Name: user_mng fk_user_device; Type: FK CONSTRAINT; Schema: zipbob_app; Owner: -
--

ALTER TABLE ONLY zipbob_app.user_mng
    ADD CONSTRAINT fk_user_device FOREIGN KEY (user_device_id) REFERENCES zipbob_app.user_device_mng(user_device_id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict dX4IfBh5ZWbpzqISQnjk9UvU5saqtt9B1dC89jFUC91SUiZYqSqwCiUyCXjIsuL

