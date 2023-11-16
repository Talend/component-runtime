/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.service;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ApplicationScoped
public class SimpleQueryLanguageCompiler {

    private static final BiPredicate<String, String> EQUAL_PREDICATE = new EqualPredicate();

    private static final BiPredicate<String, String> DIFFERENT_PREDICATE = new DifferentPredicate();

    public <T> Predicate<T> compile(final String query, final Map<String, Function<T, Object>> evaluators) {
        if (query == null || query.trim().isEmpty()) {
            return t -> true;
        }
        return doCompile(query.toCharArray(), 0, evaluators, TokenType.END).predicate;
    }

    public <T> SubExpression<T> doCompile(final char[] buffer, final int from,
            final Map<String, Function<T, Object>> evaluators, final TokenType stopToken) {
        Predicate<T> predicate = null;
        BinaryOperator<Predicate<T>> combiner = null;

        int index = from;
        while (true) {
            final Token token = nextToken(buffer, index);
            if (stopToken == token.type) {
                break;
            }

            index = moveIndex(buffer, token, true);

            switch (token.type) {
            case VALUE: {
                // expecting operator and value
                final Token opToken = nextToken(buffer, index);
                if (opToken.type != TokenType.OPERATOR) {
                    throw new IllegalArgumentException(
                            "Expected an operator after token '" + token.value + "' at index " + token.end);
                }
                index = moveIndex(buffer, opToken, true);
                final Token expectedValueToken = nextToken(buffer, index);
                if (expectedValueToken.type == TokenType.VALUE) {
                    index = moveIndex(buffer, expectedValueToken, false);
                    final Predicate<T> expr =
                            toPredicate(token.value, opToken.value, expectedValueToken.value, evaluators);

                    validateCombiner(predicate, combiner, token);
                    predicate = predicate == null ? expr : combiner.apply(predicate, expr);
                    combiner = null;
                    break;
                }
                throw new IllegalArgumentException("Unsupported token: " + token.type + " at index " + token.end);
            }
            case SUB_EXPRESSION_START:
                final SubExpression<T> expr = doCompile(buffer, index, evaluators, TokenType.SUB_EXPRESSION_END);
                validateCombiner(predicate, combiner, token);
                predicate = predicate == null ? expr.predicate : combiner.apply(predicate, expr.predicate);
                combiner = null;
                index = expr.end + 1;
                break;
            case COMBINER:
                switch (token.value) {
                case "AND":
                    combiner = Predicate::and;
                    break;
                case "OR":
                    combiner = Predicate::or;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported combiner operator: " + token.type + " at index "
                            + token.end + ", expected 'OR' or 'AND'");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported token: " + token.type + " at index " + token.end);
            }
        }
        return new SubExpression<>(index, predicate == null ? t -> true : predicate);
    }

    private <T> void validateCombiner(final Predicate<T> predicate, final BinaryOperator<Predicate<T>> combiner,
            final Token token) {
        if (combiner == null && predicate != null) {
            throw new IllegalArgumentException("Missing combiner for predicate at index " + token.end);
        }
    }

    private int moveIndex(final char[] buffer, final Token token, final boolean validate) {
        int index;
        index = token.end + 1;
        if (validate && index >= buffer.length) {
            throw new IllegalArgumentException("Unexpected token '" + token + "' at index " + token.end);
        }
        return index;
    }

    private <T> Predicate<T> toPredicate(final String key, final String operator, final String expectedValue,
            final Map<String, Function<T, Object>> evaluators) {
        final BiPredicate<String, String> comparator;
        switch (operator) {
        case "=":
            comparator = EQUAL_PREDICATE;
            break;
        case "!=":
            comparator = DIFFERENT_PREDICATE;
            break;
        default:
            throw new IllegalArgumentException("unknown operator: '" + operator + "'");
        }
        final int mapExpr = key.indexOf('[');
        if (mapExpr > 0) {
            final int endMapAccess = key.indexOf(']', mapExpr);
            if (endMapAccess > 0) {
                final String mapName = key.substring(0, mapExpr);
                final String mapKey = key.substring(mapExpr + 1, endMapAccess);
                final Function<T, Object> evaluator = ofNullable(evaluators.get(mapName))
                        .orElseThrow(() -> new IllegalArgumentException("Missing evaluator for '" + mapName + "'"));
                return new ComparePredicate<>(comparator, t -> {
                    final Object map = evaluator.apply(t);
                    if (!Map.class.isInstance(map)) {
                        throw new IllegalArgumentException(map + " is not a map");
                    }
                    return Map.class.cast(map).get(mapKey);
                }, expectedValue);
            }
        }
        final Function<T, Object> evaluator = ofNullable(evaluators.get(key))
                .orElseThrow(() -> new IllegalArgumentException("Missing evaluator for '" + key + "'"));
        return new ComparePredicate<T>(comparator, evaluator, expectedValue);
    }

    private Token nextToken(final char[] buffer, final int from) {
        if (from >= buffer.length) {
            return new Token(from, TokenType.END, null);
        }

        int actualFrom = from;
        int idx = from;
        while (idx < buffer.length) {
            switch (buffer[idx]) {
            case '(':
                if (from == idx) {
                    return new Token(idx, TokenType.SUB_EXPRESSION_START, null);
                }
                return new Token(idx - 1, TokenType.VALUE, new String(buffer, actualFrom, idx - actualFrom));
            case ')':
                if (from == idx) {
                    return new Token(idx, TokenType.SUB_EXPRESSION_END, null);
                }
                return new Token(idx - 1, TokenType.VALUE, new String(buffer, actualFrom, idx - actualFrom));
            case ' ':
                if (idx == from) { // foo = bar, we are at the whitespace before bar
                    idx++;
                    actualFrom = from + 1;
                    continue;
                }
                final String string = new String(buffer, actualFrom, idx - actualFrom);
                switch (string) {
                case "AND":
                case "OR":
                    return new Token(idx, TokenType.COMBINER, string);
                default:
                    return new Token(idx, TokenType.VALUE, string);
                }
            case '=':
                return new Token(idx, TokenType.OPERATOR, "=");
            case '!':
                idx++;
                if (idx < buffer.length && buffer[idx] == '=') {
                    return new Token(idx, TokenType.OPERATOR, "!=");
                }
                break;
            case 'A':
                if (idx == from && idx + 3 < buffer.length && buffer[idx + 1] == 'N' && buffer[idx + 2] == 'D'
                        && buffer[idx + 3] == ' ') {
                    idx += 3;
                    return new Token(idx, TokenType.COMBINER, "AND");
                }
                idx++;
                break;
            case 'O':
                if (idx == from && idx + 2 < buffer.length && buffer[idx + 1] == 'R' && buffer[idx + 2] == ' ') {
                    idx += 2;
                    return new Token(idx, TokenType.COMBINER, "OR");
                }
                idx++;
                break;
            default:
                idx++;
            }
        }
        return new Token(idx, TokenType.VALUE, new String(buffer, actualFrom, buffer.length - actualFrom));
    }

    private enum TokenType {
        SUB_EXPRESSION_START, // (
        SUB_EXPRESSION_END, // )
        VALUE, // field name or expected value
        OPERATOR, // = or !=
        COMBINER, // OR or AND
        END // EOL
    }

    @ToString
    @RequiredArgsConstructor
    private static class Token {

        private final int end;

        private final TokenType type;

        private final String value;
    }

    @ToString
    @RequiredArgsConstructor
    private static class SubExpression<T> {

        private final int end;

        private final Predicate<T> predicate;
    }

    private static class EqualPredicate implements BiPredicate<String, String> {

        @Override
        public boolean test(final String v1, final String v2) {
            return (v1 == null && "null".equals(v2)) || Objects.equals(v1, v2);
        }
    }

    private static class DifferentPredicate implements BiPredicate<String, String> {

        @Override
        public boolean test(final String v1, final String v2) {
            return !EQUAL_PREDICATE.test(v1, v2);
        }
    }

    @RequiredArgsConstructor
    private class ComparePredicate<T> implements Predicate<T> {

        private final BiPredicate<String, String> comparator;

        private final Function<T, Object> evaluator;

        private final String expectedValue;

        @Override
        public boolean test(final T t) {
            return comparator.test(String.valueOf(evaluator.apply(t)), expectedValue);
        }
    }
}
