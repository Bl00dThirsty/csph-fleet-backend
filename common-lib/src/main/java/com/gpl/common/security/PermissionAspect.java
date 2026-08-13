package com.gpl.common.security;

import com.gpl.common.exception.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Aspect AOP qui intercepte les méthodes annotées avec {@link RequiresPermission}
 * ou {@link RequiresAnyPermission} et vérifie les permissions de l'utilisateur courant
 * à partir du {@link GplSecurityContext}.
 *
 * <p>Si l'utilisateur ne possède pas les permissions requises, une
 * {@link AccessDeniedException} est levée (HTTP 403).</p>
 *
 * <p>Si aucun contexte de sécurité n'est défini (appel interne service-à-service
 * sans passer par le Gateway), la vérification est ignorée (pass-through).</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@Aspect
@Slf4j
public class PermissionAspect {

    /**
     * Intercepte les méthodes annotées avec {@link RequiresPermission}
     * et vérifie que l'utilisateur courant possède la permission requise.
     */
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                   RequiresPermission requiresPermission) throws Throwable {
        String required = requiresPermission.value();

        // Si pas de contexte de sécurité (appel interne), on laisse passer
        if (!GplSecurityContext.isAuthenticated()) {
            log.trace("No security context for @RequiresPermission('{}') on {} — internal call, pass-through",
                    required, getMethodName(joinPoint));
            return joinPoint.proceed();
        }

        if (!GplSecurityContext.hasPermission(required)) {
            String personId = GplSecurityContext.getCurrentPersonId();
            String method = getMethodName(joinPoint);
            log.warn("ACCESS DENIED — personId={}, method={}, requiredPermission={}",
                    personId, method, required);
            throw new AccessDeniedException(required,
                    String.format("Permission '%s' requise pour accéder à cette ressource", required));
        }

        log.trace("ACCESS GRANTED — personId={}, permission={}",
                GplSecurityContext.getCurrentPersonId(), required);
        return joinPoint.proceed();
    }

    /**
     * Intercepte les méthodes annotées avec {@link RequiresAnyPermission}
     * et vérifie que l'utilisateur courant possède AU MOINS UNE des permissions.
     */
    @Around("@annotation(requiresAnyPermission)")
    public Object checkAnyPermission(ProceedingJoinPoint joinPoint,
                                      RequiresAnyPermission requiresAnyPermission) throws Throwable {
        String[] required = requiresAnyPermission.value();

        // Si pas de contexte de sécurité (appel interne), on laisse passer
        if (!GplSecurityContext.isAuthenticated()) {
            log.trace("No security context for @RequiresAnyPermission on {} — internal call, pass-through",
                    getMethodName(joinPoint));
            return joinPoint.proceed();
        }

        if (!GplSecurityContext.hasAnyPermission(required)) {
            String personId = GplSecurityContext.getCurrentPersonId();
            String method = getMethodName(joinPoint);
            log.warn("ACCESS DENIED — personId={}, method={}, requiredAnyOf={}",
                    personId, method, String.join(", ", required));
            throw new AccessDeniedException(String.join("|", required),
                    String.format("Au moins une des permissions [%s] est requise", String.join(", ", required)));
        }

        log.trace("ACCESS GRANTED (any) — personId={}", GplSecurityContext.getCurrentPersonId());
        return joinPoint.proceed();
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }
}
