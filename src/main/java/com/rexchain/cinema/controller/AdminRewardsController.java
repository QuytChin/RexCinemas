package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.LoyaltyService;
import com.rexchain.cinema.service.MembershipService;
import com.rexchain.cinema.service.RewardRedemptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin/rewards")
public class AdminRewardsController {
    private final UserRepository users; private final MembershipService membership; private final LoyaltyService loyalty; private final RewardRepository rewards; private final RewardRedemptionService redemptions; private final RewardRedemptionRepository redemptionRepository; private final LoyaltyLedgerRepository ledger; private final LoyaltyPolicyAuditRepository policyAudits; private final AdminListSupport lists;
    public AdminRewardsController(UserRepository users,MembershipService membership,LoyaltyService loyalty,RewardRepository rewards,RewardRedemptionService redemptions,RewardRedemptionRepository redemptionRepository,LoyaltyLedgerRepository ledger,LoyaltyPolicyAuditRepository policyAudits,AdminListSupport lists){this.users=users;this.membership=membership;this.loyalty=loyalty;this.rewards=rewards;this.redemptions=redemptions;this.redemptionRepository=redemptionRepository;this.ledger=ledger;this.policyAudits=policyAudits;this.lists=lists;}
    @GetMapping
    String dashboard(@RequestParam Map<String, String> params, HttpServletRequest request, Model model){
        model.addAttribute("memberCount",users.count()); model.addAttribute("availablePoints",users.sumAvailablePoints()); model.addAttribute("lifetimePoints",users.sumLifetimePoints()); model.addAttribute("redeemedPoints",users.sumRedeemedPoints());
        var ledgerOptions = new AdminListSupport.Options("reference,user.email", "transactionType",
                new String[]{"createdAt", "pointsDelta", "id"}, new String[]{"user"})
                .withLabels("Loại biến động", "Mã tham chiếu hoặc email khách");
        Page<LoyaltyLedgerEntry> ledgerPage = lists.query(ledger, params, ledgerOptions);
        model.addAttribute("entries", ledgerPage.getContent());
        lists.present(model, ledgerPage, params, ledgerOptions, request,
                java.util.Arrays.stream(LoyaltyTransactionType.values()).map(Enum::name).toArray(String[]::new));
        Map<String, String> rewardParams = new HashMap<>(params);
        rewardParams.put("page", params.getOrDefault("rewardPage", "0"));
        rewardParams.put("q", params.getOrDefault("rewardQ", ""));
        rewardParams.put("status", params.getOrDefault("rewardStatus", ""));
        rewardParams.put("sort", params.getOrDefault("rewardSort", "createdAt"));
        rewardParams.put("dir", params.getOrDefault("rewardDir", "desc"));
        var rewardOptions = new AdminListSupport.Options("name", "active", "createdAt", "pointsCost", "name", "id");
        Page<Reward> rewardPage = lists.query(rewards, rewardParams, rewardOptions);
        model.addAttribute("rewards", rewardPage.getContent()); model.addAttribute("rewardPage", rewardPage);
        model.addAttribute("rewardPrev", lists.link(request, "rewardPage", rewardPage.getNumber()-1));
        model.addAttribute("rewardNext", lists.link(request, "rewardPage", rewardPage.getNumber()+1));
        Map<String, String> auditParams = new HashMap<>(params);
        auditParams.put("page", params.getOrDefault("auditPage", "0"));
        auditParams.put("q", params.getOrDefault("auditQ", ""));
        auditParams.put("sort", params.getOrDefault("auditSort", "createdAt"));
        var auditOptions = new AdminListSupport.Options("policyType,policyKey,actor", null, "createdAt", "id");
        Page<LoyaltyPolicyAudit> auditPage = lists.query(policyAudits, auditParams, auditOptions);
        model.addAttribute("policyAudits", auditPage.getContent()); model.addAttribute("auditPage", auditPage);
        model.addAttribute("auditPrev", lists.link(request, "auditPage", auditPage.getNumber()-1));
        model.addAttribute("auditNext", lists.link(request, "auditPage", auditPage.getNumber()+1));
        model.addAttribute("tierPolicies",membership.tierPolicies()); model.addAttribute("settings",membership.settings());
        model.addAttribute("redemptionCount",redemptionRepository.count()); return "admin/rewards";
    }
    @PostMapping("/settings")
    String saveSettings(@RequestParam BigDecimal amountPerPoint,@RequestParam int minimumPositivePoints,@RequestParam(defaultValue="false") boolean voucherStackingAllowed,@RequestParam(defaultValue="false") boolean redemptionEnabled,Authentication auth,RedirectAttributes redirect){try{membership.saveSettings(amountPerPoint,minimumPositivePoints,voucherStackingAllowed,redemptionEnabled,auth.getName());redirect.addFlashAttribute("success","Đã lưu cấu hình Rex Rewards");}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
    @PostMapping("/tiers/{tier}")
    String saveTier(@PathVariable MembershipTier tier,@RequestParam int qualifyingThreshold,@RequestParam BigDecimal ticketDiscountPercent,@RequestParam BigDecimal pointMultiplier,@RequestParam(defaultValue="false") boolean active,Authentication auth,RedirectAttributes redirect){try{membership.saveTierPolicy(tier,qualifyingThreshold,ticketDiscountPercent,pointMultiplier,active,auth.getName());redirect.addFlashAttribute("success","Đã lưu chính sách hạng "+tier);}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
    @PostMapping("/rewards/save")
    String saveReward(@RequestParam(required=false) Long id,@RequestParam String name,@RequestParam(required=false) String description,@RequestParam int pointsCost,@RequestParam BigDecimal voucherAmount,@RequestParam(defaultValue="0") BigDecimal voucherMinOrderAmount,@RequestParam int validityDays,@RequestParam(required=false) Integer redemptionLimit,@RequestParam(defaultValue="false") boolean active,RedirectAttributes redirect){try{redemptions.saveReward(id,name,description,pointsCost,voucherAmount,voucherMinOrderAmount,validityDays,redemptionLimit,active);redirect.addFlashAttribute("success","Đã lưu ưu đãi");}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
    @PostMapping("/rewards/{id}/toggle")
    String toggleReward(@PathVariable Long id,RedirectAttributes redirect){Reward reward=rewards.findById(id).orElseThrow();reward.setActive(!reward.isActive());rewards.save(reward);redirect.addFlashAttribute("success","Đã cập nhật trạng thái ưu đãi");return "redirect:/admin/rewards";}
    @PostMapping("/adjust")
    String adjust(@RequestParam Long userId,@RequestParam int points,@RequestParam String reason,Authentication auth,RedirectAttributes redirect){try{loyalty.adjustAvailablePoints(userId,points,reason,auth.getName());redirect.addFlashAttribute("success","Đã ghi nhận điều chỉnh điểm và ledger");}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
}
