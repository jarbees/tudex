//  Created on 12.09.26.

import SwiftUI
import SwiftUIIntrospect

extension View {
    @ViewBuilder
    func tabViewCrossfade() -> some View {
        if #available(iOS 27.0, *) {
            introspect(.tabView, on: .iOS(.v27)) { tabBarController in
                TabCrossfadeInstaller.install(on: tabBarController)
            }
        } else {
            self
        }
    }
}

@available(iOS 27.0, *)
@MainActor
private enum TabCrossfadeInstaller {
    private static var associationKey: UInt8 = 0

    static func install(on tabBarController: UITabBarController) {
        if objc_getAssociatedObject(
            tabBarController,
            &associationKey
        ) is TabCrossfadeDelegate {
            return
        }

        let delegate = TabCrossfadeDelegate()

        objc_setAssociatedObject(
            tabBarController,
            &associationKey,
            delegate,
            .OBJC_ASSOCIATION_RETAIN_NONATOMIC
        )

        tabBarController.delegate = delegate
    }
}

@available(iOS 27.0, *)
@MainActor
private final class TabCrossfadeDelegate:
    NSObject,
    UITabBarControllerDelegate
{
    func tabBarController(
        _ tabBarController: UITabBarController,
        animationControllerForTransitionFrom fromVC: UIViewController,
        to toVC: UIViewController
    ) -> UIViewControllerAnimatedTransitioning? {
        TabCrossfadeAnimator()
    }
}

@available(iOS 27.0, *)
private final class TabCrossfadeAnimator:
    NSObject,
    UIViewControllerAnimatedTransitioning
{
    func transitionDuration(
        using transitionContext: UIViewControllerContextTransitioning?
    ) -> TimeInterval {
        0.125
    }

    func animateTransition(
        using context: UIViewControllerContextTransitioning
    ) {
        guard
            let fromView = context.view(forKey: .from),
            let toView = context.view(forKey: .to),
            let toVC = context.viewController(forKey: .to),
            let snapshot = fromView.snapshotView(afterScreenUpdates: false)
        else {
            context.completeTransition(false)
            return
        }

        let container = context.containerView
        let finalFrame = context.finalFrame(for: toVC)

        snapshot.frame = fromView.frame
        snapshot.isUserInteractionEnabled = false

        toView.frame = finalFrame
        toView.transform = CGAffineTransform(
            scaleX: 0.995,
            y: 0.995
        )

        container.insertSubview(toView, aboveSubview: fromView)

        // Finish the actual tab transition immediately so that the
        // destination remains interactive during the visual transition.
        context.completeTransition(true)

        container.addSubview(snapshot)

        UIView.animate(
            withDuration: transitionDuration(using: context),
            delay: 0,
            options: [
                .curveEaseInOut,
                .beginFromCurrentState,
                .allowUserInteraction
            ]
        ) {
            snapshot.alpha = 0
            snapshot.transform = CGAffineTransform(
                scaleX: 0.995,
                y: 0.995
            )

            toView.transform = .identity
        } completion: { _ in
            snapshot.removeFromSuperview()
            toView.transform = .identity
        }
    }
}
