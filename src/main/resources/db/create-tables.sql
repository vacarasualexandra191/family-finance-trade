-- Tabele pentru Family Finance App
-- Ruleaza pe conexiunea family_budget din SQL Developer

-- Tranzactii financiare
CREATE TABLE ff_transactions (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR2(20) NOT NULL,
    sub_category VARCHAR2(30) NOT NULL,
    amount NUMBER(12,2) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR2(200) NOT NULL,
    currency VARCHAR2(3) DEFAULT 'RON'
);

-- Tinte de economisire lunare
CREATE TABLE ff_savings_goals (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    year NUMBER NOT NULL,
    month NUMBER NOT NULL,
    monthly_income NUMBER(12,2) NOT NULL,
    savings_percentage NUMBER NOT NULL,
    notes VARCHAR2(255),
    CONSTRAINT uq_savings_goal UNIQUE (year, month)
);

-- Fonduri de investitii
CREATE TABLE ff_investment_funds (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol VARCHAR2(20) NOT NULL UNIQUE,
    name VARCHAR2(150) NOT NULL,
    fund_type VARCHAR2(20) NOT NULL,
    risk_level VARCHAR2(20) NOT NULL,
    time_horizon VARCHAR2(20) NOT NULL,
    current_price NUMBER(12,4),
    currency VARCHAR2(5) DEFAULT 'USD',
    last_price_update TIMESTAMP,
    description VARCHAR2(500),
    annual_return_pct NUMBER(6,2)
);

-- Tranzactii de investitii (cumparare/vanzare unitati)
CREATE TABLE ff_investment_transactions (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fund_id NUMBER NOT NULL,
    operation_type VARCHAR2(10) NOT NULL,
    units NUMBER(12,4) NOT NULL,
    price_per_unit NUMBER(12,4) NOT NULL,
    transaction_date DATE NOT NULL,
    total_value NUMBER(12,2) NOT NULL,
    currency VARCHAR2(5) DEFAULT 'USD',
    CONSTRAINT fk_inv_tx_fund FOREIGN KEY (fund_id) REFERENCES ff_investment_funds(id)
);

-- Date initiale: fonduri populare
INSERT INTO ff_investment_funds (symbol, name, fund_type, risk_level, time_horizon, currency, annual_return_pct, description)
VALUES ('SPY', 'SPDR S&P 500 ETF Trust', 'ETF', 'MEDIU', 'LUNG_7_ANI_PLUS', 'USD', 10.5,
        'ETF care urmareste indicele S&P 500, format din cele mai mari 500 companii americane. Diversificare excelenta.');

INSERT INTO ff_investment_funds (symbol, name, fund_type, risk_level, time_horizon, currency, annual_return_pct, description)
VALUES ('VOO', 'Vanguard S&P 500 ETF', 'ETF', 'MEDIU', 'LUNG_7_ANI_PLUS', 'USD', 10.3,
        'ETF Vanguard cu comisioane foarte mici (0.03%). Una dintre cele mai populare optiuni pentru investitori pe termen lung.');

INSERT INTO ff_investment_funds (symbol, name, fund_type, risk_level, time_horizon, currency, annual_return_pct, description)
VALUES ('QQQ', 'Invesco QQQ Trust', 'ETF', 'RIDICAT', 'LUNG_7_ANI_PLUS', 'USD', 14.2,
        'ETF care urmareste Nasdaq-100, dominat de companii tech (Apple, Microsoft, Google, Amazon). Risc mai mare, randament mai mare.');

INSERT INTO ff_investment_funds (symbol, name, fund_type, risk_level, time_horizon, currency, annual_return_pct, description)
VALUES ('BND', 'Vanguard Total Bond Market ETF', 'ETF', 'SCAZUT', 'SCURT_1_3_ANI', 'USD', 4.5,
        'ETF de obligatiuni americane. Risc scazut, randament stabil. Ideal pentru echilibrarea unui portofoliu agresiv.');

INSERT INTO ff_investment_funds (symbol, name, fund_type, risk_level, time_horizon, currency, annual_return_pct, description)
VALUES ('VWCE.DE', 'Vanguard FTSE All-World UCITS ETF', 'ETF', 'MEDIU', 'LUNG_7_ANI_PLUS', 'EUR', 9.8,
        'ETF global UCITS (disponibil in Europa), acoperire 3700+ companii din intreaga lume. Diversificare maxima.');

INSERT INTO ff_investment_funds (symbol, name, fund_type, risk_level, time_horizon, currency, annual_return_pct, description)
VALUES ('XDWD.DE', 'iShares MSCI World UCITS ETF', 'ETF', 'MEDIU', 'MEDIU_3_7_ANI', 'EUR', 9.2,
        'ETF pe indicele MSCI World (tarile dezvoltate). Popular printre investitorii europeni. Denominat in EUR.');

COMMIT;
