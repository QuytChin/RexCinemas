package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.MembershipService;
import com.rexchain.cinema.service.RewardRedemptionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/rewards")
public class RewardsController {
    private final UserRepository users; private final RewardRepository rewards; private final RewardRedemptionRepository redemptions;
    private final LoyaltyLedgerRepository ledger; private final MembershipService membership; private final RewardRedemptionService redemptionService;
    public RewardsController(UserRepository users, RewardRepository rewards, RewardRedemptionRepository redemptions, LoyaltyLedgerRepository ledger, MembershipService membership, RewardRedemptionService redemptionService) {
        this.users=users; this.rewards=rewards; this.redemptions=redemptions; this.ledger=ledger; this.membership=membership; this.redemptionService=redemptionService;
    }
    @GetMapping
    @Transactional(readOnly = true)
    String dashboard(@RequestParam(defaultValue="0") int page, @RequestParam(required=false) String tab, Authentication auth, Model model) {
        User user=current(auth); var history=typesFor(tab)==null ? ledger.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(Math.max(0,page),10)) : ledger.findByUserIdAndTransactionTypeInOrderByCreatedAtDesc(user.getId(),typesFor(tab),PageRequest.of(Math.max(0,page),10));
        var membershipStatus = membership.statusFor(user.getTierQualifyingPoints());
        model.addAttribute("rewardsUser",user); model.addAttribute("membershipStatus",membershipStatus);
        model.addAttribute("currentTier",membershipStatus.tier()); model.addAttribute("currentPolicy",membershipStatus.policy());
        model.addAttribute("tierPolicies",membership.tierPolicies().stream().filter(MembershipTierPolicy::isActive).toList());
        model.addAttribute("tierProgress",membershipStatus.progressPercent());
        model.addAttribute("nextThreshold",membershipStatus.nextThreshold() == null ? membershipStatus.qualifyingPoints() : membershipStatus.nextThreshold());
        model.addAttribute("pointsToNext",membershipStatus.pointsToNext());
        model.addAttribute("settings",membership.settings()); model.addAttribute("rewardCatalog",rewards.findByActiveTrueOrderByPointsCostAsc()); model.addAttribute("redemptions",redemptions.findTop10ByUserIdOrderByCreatedAtDesc(user.getId()));
        model.addAttribute("history",history); model.addAttribute("historyTab",tab==null?"all":tab); return "rewards/dashboard";
    }
    @PostMapping("/redeem")
    String redeem(@RequestParam Long rewardId,@RequestParam String requestId,Authentication auth,RedirectAttributes redirect){
        try{RewardRedemption redemption=redemptionService.redeem(current(auth),rewardId,requestId);redirect.addFlashAttribute("success","Đổi điểm thành công. Mã voucher của bạn: "+redemption.getVoucher().getCode());}
        catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());} return "redirect:/rewards";
    }
    private User current(Authentication auth){if(auth==null||!auth.isAuthenticated())throw new IllegalStateException("Bạn cần đăng nhập");return users.findByEmailIgnoreCase(auth.getName()).orElseThrow();}
    private List<LoyaltyTransactionType> typesFor(String tab){if(tab==null||tab.equals("all"))return null;return switch(tab){case "earn"->List.of(LoyaltyTransactionType.EARN);case "redeem"->List.of(LoyaltyTransactionType.REDEEM);case "reverse"->List.of(LoyaltyTransactionType.REVERSE_EARN,LoyaltyTransactionType.REFUND_REDEMPTION);default->null;};}
}
